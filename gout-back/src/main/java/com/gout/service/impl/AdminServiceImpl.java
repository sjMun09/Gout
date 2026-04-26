package com.gout.service.impl;

import com.gout.dao.UserRepository;
import com.gout.dto.response.AdminUserResponse;
import com.gout.entity.User;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.AdminService;
import com.gout.util.LogMasks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 유스케이스 구현.
 *
 * 설계 결정:
 *  - User.status 필드는 Agent-H 가 V23 마이그레이션으로 users.status 컬럼을 추가한 뒤 엔티티에 올릴 예정.
 *    엔티티에 먼저 올리면 빌드 충돌 → native SQL 로 읽고 쓴다.
 *  - Post.status / Comment.status 는 이미 컬럼 + 엔티티 모두 존재.
 *    하지만 엔티티 메서드(delete()) 는 "DELETED" 로만 세팅하므로 HIDDEN 용 메서드가 없다 →
 *    엔티티를 건드리지 않기 위해 이쪽도 native UPDATE 로 통일.
 *  - users.status 가 아직 없을 수 있는 환경(현재 HEAD)에서는 suspend/unsuspend 가 SQL 에러로 실패할 수 있다.
 *    그 경우 BusinessException 으로 래핑해 500 대신 명확한 메시지를 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    // ===== Users =====

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> searchUsers(String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String trimmed = keyword == null ? null : keyword.trim();
        boolean hasKeyword = trimmed != null && !trimmed.isEmpty();

        // users.status 컬럼이 아직 없는 환경을 위해 COALESCE 대신 COLUMN 존재 여부를 information_schema 로 확인
        boolean statusColumnExists = userHasStatusColumn();

        String baseSql = hasKeyword
                ? "FROM users WHERE (LOWER(nickname) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?))"
                : "FROM users";
        Object[] baseArgs = hasKeyword
                ? new Object[] { "%" + trimmed + "%", "%" + trimmed + "%" }
                : new Object[] {};

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + baseSql,
                Long.class,
                baseArgs
        );

        String selectCols = statusColumnExists
                ? "id, status"
                : "id, 'ACTIVE' AS status";

        String listSql = "SELECT " + selectCols + " " + baseSql
                + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        Object[] listArgs = new Object[baseArgs.length + 2];
        System.arraycopy(baseArgs, 0, listArgs, 0, baseArgs.length);
        listArgs[baseArgs.length] = safeSize;
        listArgs[baseArgs.length + 1] = (long) safePage * safeSize;

        Map<String, String> statusById = new HashMap<>();
        jdbcTemplate.query(listSql, rs -> {
            statusById.put(rs.getString("id"), rs.getString("status"));
        }, listArgs);

        if (statusById.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total == null ? 0 : total);
        }

        // 엔티티로 한 번 더 로드해서 nickname/email/role/createdAt 을 안전하게 매핑
        List<User> users = userRepository.findAllById(statusById.keySet());
        // createdAt DESC 재정렬 (findAllById 는 순서 보장 X)
        users = users.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        List<AdminUserResponse> content = users.stream()
                .map(u -> AdminUserResponse.of(u, statusById.get(u.getId())))
                .toList();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    @Transactional
    public void suspendUser(String userId) {
        requireUser(userId);
        updateUserStatus(userId, "SUSPENDED");
    }

    @Override
    @Transactional
    public void unsuspendUser(String userId) {
        requireUser(userId);
        updateUserStatus(userId, "ACTIVE");
    }

    @Override
    @Transactional
    public void promoteUser(String userId) {
        requireUser(userId);
        // role 은 PostgreSQL ENUM(user_role) — 캐스팅 필요
        int updated = jdbcTemplate.update(
                "UPDATE users SET role = CAST(? AS user_role), updated_at = NOW() WHERE id = ?",
                "ADMIN", userId
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    // ===== Posts =====

    @Override
    @Transactional
    public void hidePost(String postId) {
        int updated = jdbcTemplate.update(
                "UPDATE posts SET status = 'HIDDEN', updated_at = NOW() WHERE id = ?",
                postId
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void deletePost(String postId) {
        int updated = jdbcTemplate.update(
                "UPDATE posts SET status = 'DELETED', updated_at = NOW() WHERE id = ?",
                postId
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
    }

    // ===== Comments =====

    @Override
    @Transactional
    public void deleteComment(String commentId) {
        int updated = jdbcTemplate.update(
                "UPDATE comments SET status = 'DELETED', updated_at = NOW() WHERE id = ?",
                commentId
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    // ===== helpers =====

    private void requireUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void updateUserStatus(String userId, String status) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE users SET status = ?, updated_at = NOW() WHERE id = ?",
                    status, userId
            );
            if (updated == 0) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
        } catch (DataAccessException e) {
            // users.status 컬럼이 아직 없는 환경 (Agent-H V23 머지 전)
            log.warn("users.status 컬럼 업데이트 실패 — Agent-H V23 머지 대기 중. userId={}, status={}",
                    LogMasks.maskUserId(userId), status, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "users.status 컬럼 미존재 — V23 마이그레이션 이후 재시도하세요.");
        }
    }

    private boolean userHasStatusColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_name = 'users' AND column_name = 'status'",
                    Integer.class
            );
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.warn("information_schema 조회 실패 — status 컬럼 존재 여부 불명, false 로 가정", e);
            return false;
        }
    }
}
