package com.gout.service.impl;

import com.gout.dao.CommentRepository;
import com.gout.dao.PostBookmarkRepository;
import com.gout.dao.PostLikeRepository;
import com.gout.dao.PostRepository;
import com.gout.dto.request.CreatePostRequest;
import com.gout.dto.response.CommentResponse;
import com.gout.dto.response.PostDetailResponse;
import com.gout.dto.response.PostSummaryResponse;
import com.gout.entity.Comment;
import com.gout.entity.Post;
import com.gout.entity.PostLike;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.PostService;
import com.gout.service.UserNicknameResolver;
import com.gout.service.event.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    // V24 FK 제거 후 탈퇴(DELETED) 사용자 닉네임을 "탈퇴한 사용자" 로 표기하기 위한 공통 해석기.
    private final UserNicknameResolver userNicknameResolver;
    // P1-10 (P0-G): toggleLike 알림을 @TransactionalEventListener(AFTER_COMMIT) 으로 분리.
    private final ApplicationEventPublisher eventPublisher;

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
        Map<String, String> nicknameMap = userNicknameResolver.loadNicknames(userIds);

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
            String nickname = userNicknameResolver.resolve(nicknameMap, post.getUserId());
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

        // P1-10: viewCount 는 JPA dirty-checking 이 아닌 아토믹 UPDATE 로 증가.
        // 기존 post.incrementViewCount() + dirty-checking 방식은 동일 post 동시 조회 시
        // last-write-wins 로 카운트 손실이 발생한다 (SELECT-then-UPDATE race).
        // incrementViewCount 는 `UPDATE ... SET viewCount = viewCount + 1` 1-shot 이라
        // DB 레벨 row lock 으로 경합이 해소된다.
        // clearAutomatically=true 로 컨텍스트를 비우므로 in-memory post 는 detached —
        // 응답에 반영될 viewCount 만 로컬 필드에서 +1 동기화.
        postRepository.incrementViewCount(post.getId());
        post.incrementViewCount();

        List<Comment> comments = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), "VISIBLE");

        Set<String> userIds = new HashSet<>();
        if (!post.isAnonymous()) userIds.add(post.getUserId());
        comments.stream()
                .filter(c -> !c.isAnonymous())
                .forEach(c -> userIds.add(c.getUserId()));
        Map<String, String> nicknameMap = userNicknameResolver.loadNicknames(userIds);

        List<CommentResponse> commentResponses = comments.stream()
                .map(c -> CommentResponse.of(c,
                        userNicknameResolver.resolve(nicknameMap, c.getUserId())))
                .toList();

        boolean liked = currentUserId != null
                && postLikeRepository.existsByIdPostIdAndIdUserId(post.getId(), currentUserId);

        long bookmarkCount = postBookmarkRepository.countByPostId(post.getId());
        boolean bookmarked = currentUserId != null
                && postBookmarkRepository.existsByUserIdAndPostId(currentUserId, post.getId());

        String nickname = userNicknameResolver.resolve(nicknameMap, post.getUserId());
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
        String nickname = post.isAnonymous() ? null : userNicknameResolver.resolve(userId);
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
        Map<String, String> nicknameMap = userNicknameResolver.loadNicknames(userIds);

        List<CommentResponse> commentResponses = comments.stream()
                .map(c -> CommentResponse.of(c,
                        userNicknameResolver.resolve(nicknameMap, c.getUserId())))
                .toList();

        boolean liked = postLikeRepository.existsByIdPostIdAndIdUserId(post.getId(), userId);
        long bookmarkCount = postBookmarkRepository.countByPostId(post.getId());
        boolean bookmarked = postBookmarkRepository.existsByUserIdAndPostId(userId, post.getId());
        String nickname = userNicknameResolver.resolve(nicknameMap, post.getUserId());
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

        // P1-10: 알림을 좋아요 트랜잭션에서 분리한다.
        // - publishEvent 는 동기 호출이지만, listener 가 @TransactionalEventListener(AFTER_COMMIT) 이므로
        //   실제 알림 저장은 좋아요 커밋 이후에 실행된다.
        // - 알림 저장 실패는 좋아요 롤백을 유발하지 않는다 (이미 커밋됨).
        if (!exists) {
            eventPublisher.publishEvent(new PostLikedEvent(
                    post.getId(), post.getUserId(), userId, post.getTitle()));
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

    // 닉네임 조회 로직은 UserNicknameResolver 로 통합. DELETED 사용자 / 고아 userId 를
    // 일관되게 "탈퇴한 사용자" 로 치환한다. (V24 FK 제거 리팩터링 일환)
}
