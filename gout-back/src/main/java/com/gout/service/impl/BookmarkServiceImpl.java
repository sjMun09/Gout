package com.gout.service.impl;

import com.gout.dao.CommentRepository;
import com.gout.dao.PostBookmarkRepository;
import com.gout.dao.PostRepository;
import com.gout.dao.UserRepository;
import com.gout.dto.response.PostSummaryResponse;
import com.gout.entity.Post;
import com.gout.entity.PostBookmark;
import com.gout.entity.User;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final PostBookmarkRepository postBookmarkRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public boolean toggle(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if ("DELETED".equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        Optional<PostBookmark> existing =
                postBookmarkRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            postBookmarkRepository.delete(existing.get());
            return false;
        }
        postBookmarkRepository.save(PostBookmark.of(userId, postId));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getMyBookmarks(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : size);
        Page<PostBookmark> bookmarks =
                postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<String> postIds = bookmarks.getContent().stream()
                .map(PostBookmark::getPostId)
                .toList();

        if (postIds.isEmpty()) {
            return bookmarks.map(b -> null);
        }

        // 게시글 배치 조회
        Map<String, Post> postMap = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        // 작성자 닉네임 배치 조회 (익명 제외)
        Set<String> authorIds = postMap.values().stream()
                .filter(p -> !p.isAnonymous())
                .map(Post::getUserId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, String> nicknameMap = loadNicknames(authorIds);

        // 댓글 수 배치 조회 — N+1 제거 (PostServiceImpl.commentCountMap 과 동일 패턴).
        // 기존: 루프 내 countByPostIdAndStatus 를 건당 1회 → 페이지 N 건이면 N 쿼리.
        // 변경: IN (...) GROUP BY postId 로 1회 조회 → getOrDefault 로 0 처리.
        Map<String, Long> commentCountMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : commentRepository.countByPostIdInAndStatusGroupByPostId(postIds, "VISIBLE")
                        .stream()
                        .collect(Collectors.toMap(
                                CommentRepository.PostCommentCount::getPostId,
                                CommentRepository.PostCommentCount::getCnt,
                                (a, b) -> a));

        return bookmarks.map(bookmark -> {
            Post post = postMap.get(bookmark.getPostId());
            if (post == null || "DELETED".equals(post.getStatus())) {
                // 삭제된 게시글은 자리표시용 최소 정보 대신 null 로 필터링하는 편이 나으나
                // Page.map 은 null 을 돌려주면 content 에 null 이 섞인다. 여기선 placeholder 리턴.
                return null;
            }
            int commentCount = commentCountMap.getOrDefault(post.getId(), 0L).intValue();
            String nickname = nicknameMap.getOrDefault(post.getUserId(), "알 수 없음");
            return PostSummaryResponse.of(post, commentCount, nickname);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBookmarked(String postId, String userId) {
        return postBookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }

    private Map<String, String> loadNicknames(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }
}
