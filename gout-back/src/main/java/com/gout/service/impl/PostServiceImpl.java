package com.gout.service.impl;

import com.gout.dao.CommentRepository;
import com.gout.dao.PostBookmarkRepository;
import com.gout.dao.PostLikeRepository;
import com.gout.dao.PostRepository;
import com.gout.dao.UserRepository;
import com.gout.dto.request.CreatePostRequest;
import com.gout.dto.response.CommentResponse;
import com.gout.dto.response.PostDetailResponse;
import com.gout.dto.response.PostSummaryResponse;
import com.gout.entity.Comment;
import com.gout.entity.Post;
import com.gout.entity.PostLike;
import com.gout.entity.User;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.NotificationService;
import com.gout.service.PostService;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(String category, int page, int size) {
        return getPosts(category, null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(String category, String keyword, int page, int size) {
        Post.PostCategory categoryEnum = parseCategory(category);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : size);
        // null-keyword 바인딩 시 PG 드라이버가 bytea 로 추론하는 이슈 회피 위해 빈 문자열로 coalesce.
        String safeKeyword = keyword == null ? "" : keyword.trim();
        Page<Post> posts = postRepository.searchVisible(categoryEnum, safeKeyword, pageable);

        Set<String> userIds = posts.getContent().stream()
                .filter(p -> !p.isAnonymous())
                .map(Post::getUserId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, String> nicknameMap = loadNicknames(userIds);

        // P1-7: 페이지 내 전체 postId 에 대해 댓글 수를 1회 GROUP BY 쿼리로 조회.
        // 기존: 페이지 건수(N) 만큼 COUNT 쿼리 → 20 round-trip.
        // 변경: IN (...) GROUP BY postId → 1 round-trip. 0개 post 는 map 미포함 → getOrDefault 로 처리.
        // loadNicknames 는 이미 findAllById 로 배치 조회 중이므로 그대로 둔다.
        List<String> postIds = posts.getContent().stream()
                .map(Post::getId)
                .toList();
        Map<String, Long> commentCountMap = commentCountMap(postIds);

        return posts.map(post -> {
            int commentCount = commentCountMap.getOrDefault(post.getId(), 0L).intValue();
            String nickname = nicknameMap.getOrDefault(post.getUserId(), "알 수 없음");
            return PostSummaryResponse.of(post, commentCount, nickname);
        });
    }

    private Map<String, Long> commentCountMap(List<String> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentRepository
                .countByPostIdInAndStatusGroupByPostId(postIds, "VISIBLE")
                .stream()
                .collect(Collectors.toMap(
                        CommentRepository.PostCommentCount::getPostId,
                        CommentRepository.PostCommentCount::getCnt,
                        (a, b) -> a));
    }

    @Override
    @Transactional
    public PostDetailResponse getPost(String id, String currentUserId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if ("DELETED".equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        post.incrementViewCount();

        List<Comment> comments = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), "VISIBLE");

        Set<String> userIds = new HashSet<>();
        if (!post.isAnonymous()) userIds.add(post.getUserId());
        comments.stream()
                .filter(c -> !c.isAnonymous())
                .forEach(c -> userIds.add(c.getUserId()));
        Map<String, String> nicknameMap = loadNicknames(userIds);

        List<CommentResponse> commentResponses = comments.stream()
                .map(c -> CommentResponse.of(c,
                        nicknameMap.getOrDefault(c.getUserId(), "알 수 없음")))
                .toList();

        boolean liked = currentUserId != null
                && postLikeRepository.existsByIdPostIdAndIdUserId(post.getId(), currentUserId);

        long bookmarkCount = postBookmarkRepository.countByPostId(post.getId());
        boolean bookmarked = currentUserId != null
                && postBookmarkRepository.existsByUserIdAndPostId(currentUserId, post.getId());

        String nickname = nicknameMap.getOrDefault(post.getUserId(), "알 수 없음");
        return PostDetailResponse.of(post, nickname, liked, bookmarkCount, bookmarked,
                commentResponses);
    }

    @Override
    @Transactional
    public PostSummaryResponse createPost(String userId, CreatePostRequest request) {
        Post post = Post.builder()
                .userId(userId)
                .category(parseCategory(request.getCategory()))
                .title(request.getTitle())
                .content(request.getContent())
                .isAnonymous(request.isAnonymous())
                .imageUrls(request.getImageUrls())
                .build();

        Post saved = postRepository.save(post);
        String nickname = post.isAnonymous() ? null : findNickname(userId);
        return PostSummaryResponse.of(saved, 0, nickname);
    }

    @Override
    @Transactional
    public PostDetailResponse updatePost(String id, String userId, CreatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if ("DELETED".equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        post.updateContent(request.getTitle(), request.getContent());
        post.replaceImageUrls(request.getImageUrls());

        List<Comment> comments = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), "VISIBLE");
        Set<String> userIds = new HashSet<>();
        if (!post.isAnonymous()) userIds.add(post.getUserId());
        comments.stream()
                .filter(c -> !c.isAnonymous())
                .forEach(c -> userIds.add(c.getUserId()));
        Map<String, String> nicknameMap = loadNicknames(userIds);

        List<CommentResponse> commentResponses = comments.stream()
                .map(c -> CommentResponse.of(c,
                        nicknameMap.getOrDefault(c.getUserId(), "알 수 없음")))
                .toList();

        boolean liked = postLikeRepository.existsByIdPostIdAndIdUserId(post.getId(), userId);
        long bookmarkCount = postBookmarkRepository.countByPostId(post.getId());
        boolean bookmarked = postBookmarkRepository.existsByUserIdAndPostId(userId, post.getId());
        String nickname = nicknameMap.getOrDefault(post.getUserId(), "알 수 없음");
        return PostDetailResponse.of(post, nickname, liked, bookmarkCount, bookmarked,
                commentResponses);
    }

    @Override
    @Transactional
    public void deletePost(String id, String userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        post.delete();
    }

    @Override
    @Transactional
    public void toggleLike(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if ("DELETED".equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        boolean exists = postLikeRepository.existsByIdPostIdAndIdUserId(postId, userId);
        if (exists) {
            postLikeRepository.deleteByIdPostIdAndIdUserId(postId, userId);
            post.toggleLike(false);
        } else {
            postLikeRepository.save(PostLike.of(postId, userId));
            post.toggleLike(true);
        }

        // === 알림 트리거 (Agent-G / feature/notifications) ===
        // 좋아요 '추가' 시점에만 게시글 작성자에게 POST_LIKE (자기 좋아요 제외)
        if (!exists && !post.getUserId().equals(userId)) {
            notificationService.createFor(
                    post.getUserId(),
                    "POST_LIKE",
                    "게시글에 좋아요가 눌렸습니다",
                    post.getTitle(),
                    "/community/" + post.getId());
        }
    }

    private Post.PostCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return Post.PostCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 카테고리: " + category);
        }
    }

    private Map<String, String> loadNicknames(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }

    private String findNickname(String userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .orElse("알 수 없음");
    }
}
