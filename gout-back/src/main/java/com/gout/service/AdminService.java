package com.gout.service;

import com.gout.dto.response.AdminUserResponse;
import org.springframework.data.domain.Page;

/**
 * 관리자 전용 유스케이스. User/Post/Comment 엔티티에 status 필드가 없거나
 * 다른 에이전트의 머지를 기다리는 상황이라 native SQL 로 상태를 갱신한다.
 */
public interface AdminService {

    // ===== Users =====

    Page<AdminUserResponse> searchUsers(String keyword, int page, int size);

    void suspendUser(String userId);

    void unsuspendUser(String userId);

    void promoteUser(String userId);

    // ===== Posts =====

    void hidePost(String postId);

    void deletePost(String postId);

    // ===== Comments =====

    void deleteComment(String commentId);
}
