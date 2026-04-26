package com.gout.service.impl;

import com.gout.dao.CommentRepository;
import com.gout.dao.PostBookmarkRepository;
import com.gout.dao.PostHashtagRepository;
import com.gout.dao.PostLikeRepository;
import com.gout.dao.PostRepository;
import com.gout.dto.request.CreatePostRequest;
import com.gout.dto.response.CommentResponse;
import com.gout.dto.response.PostDetailResponse;
import com.gout.dto.response.PostSummaryResponse;
import com.gout.entity.Comment;
import com.gout.entity.Post;
import com.gout.entity.PostHashtag;
import com.gout.entity.PostLike;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.page.PageablePolicy;
import com.gout.service.PostService;
import com.gout.service.UserNicknameResolver;
import com.gout.service.event.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private final PostHashtagRepository postHashtagRepository;
    // V24 FK 제거 후 탈퇴(DELETED) 사용자 닉네임을 "탈퇴한 사용자" 로 표기하기 위한 공통 해석기.
    private final UserNicknameResolver userNicknameResolver;
    // P1-10 (P0-G): toggleLike 알림을 @TransactionalEventListener(AFTER_COMMIT) 으로 분리.
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(String category, int page, int size) {
        return getPosts(category, null, null, null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(String category, String keyword, int page, int size) {
        return getPosts(category, keyword, null, null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(String category, String keyword, String sort, int page, int size) {
        return getPosts(category, keyword, sort, null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(String category, String keyword, String sort, String tag,
                                              int page, int size) {
        // 방어 심층화: 컨트롤러 외 경로(테스트/내부 호출)에서도 정책 보장.
        Pageable pageable = PageablePolicy.POST.toPageable(page, size, resolveSort(sort));

        // 태그 필터가 있으면 해당 태그 postId 집합으로 pre-filter 후 sort 적용.
        if (tag != null && !tag.isBlank()) {
            Set<String> tagPostIds = postHashtagRepository.findPostIdsByTag(tag.trim());
            if (tagPostIds.isEmpty()) {
                return Page.empty(pageable);
            }
            // ID 집합으로 VISIBLE 게시글을 페이징 조회 (Pageable.Sort 적용됨)
            Page<Post> posts = postRepository.findVisibleByIds(tagPostIds, pageable);
            return buildSummaryPage(posts);
        }

        Post.PostCategory categoryEnum = parseCategory(category);
        // null-keyword 바인딩 시 PG 드라이버가 bytea 로 추론하는 이슈 회피 위해 빈 문자열로 coalesce.
        String safeKeyword = keyword == null ? "" : keyword.trim();
        Page<Post> posts = postRepository.searchVisible(categoryEnum, safeKeyword, pageable);
        return buildSummaryPage(posts);
    }

    private Page<PostSummaryResponse> buildSummaryPage(Page<Post> posts) {
        Set<String> userIds = posts.getContent().stream()
                .filter(p -> !p.isAnonymous())
                .map(Post::getUserId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, String> nicknameMap = userNicknameResolver.loadNicknames(userIds);

        // P1-7: 페이지 내 전체 postId 에 대해 댓글 수를 1회 GROUP BY 쿼리로 조회.
        // 기존: 페이지 건수(N) 만큼 COUNT 쿼리 → 20 round-trip.
        // 변경: IN (...) GROUP BY postId → 1 round-trip.
        List<String> postIds = posts.getContent().stream()
                .map(Post::getId)
                .toList();
        Map<String, Long> commentCountMap = commentCountMap(postIds);

        // 태그도 1회 IN 배치 조회 (N+1 방지)
        Map<String, List<String>> tagMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : postHashtagRepository.findByPostIdIn(postIds).stream()
                        .collect(Collectors.groupingBy(
                                PostHashtag::getPostId,
                                Collectors.mapping(PostHashtag::getTag, Collectors.toList())));

        return posts.map(post -> {
            int commentCount = commentCountMap.getOrDefault(post.getId(), 0L).intValue();
            String nickname = userNicknameResolver.resolve(nicknameMap, post.getUserId());
            List<String> tags = tagMap.getOrDefault(post.getId(), List.of());
            return PostSummaryResponse.of(post, commentCount, nickname, tags);
        });
    }

    private Map<String, Long> commentCountMap(List<String> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentRepository
                .countByPostIdInAndStatusGroupByPostId(postIds, Comment.Status.VISIBLE)
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

        if (post.getStatus() == Post.Status.DELETED) {
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
                .findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), Comment.Status.VISIBLE);

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

        List<String> tags = postHashtagRepository.findByPostId(post.getId()).stream()
                .map(PostHashtag::getTag)
                .toList();

        String nickname = userNicknameResolver.resolve(nicknameMap, post.getUserId());
        return PostDetailResponse.of(post, nickname, liked, bookmarkCount, bookmarked,
                commentResponses, tags);
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

        // 중복 제거 후 태그 저장 (LinkedHashSet 으로 입력 순서 유지)
        List<String> savedTags = saveHashtags(saved.getId(), request.getTags());

        String nickname = post.isAnonymous() ? null : userNicknameResolver.resolve(userId);
        return PostSummaryResponse.of(saved, 0, nickname, savedTags);
    }

    @Override
    @Transactional
    public PostDetailResponse updatePost(String id, String userId, CreatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (post.getStatus() == Post.Status.DELETED) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        post.updateContent(request.getTitle(), request.getContent());
        post.replaceImageUrls(request.getImageUrls());

        // 태그 재설정: 기존 전부 삭제 후 재삽입
        postHashtagRepository.deleteAllByPostId(post.getId());
        List<String> savedTags = saveHashtags(post.getId(), request.getTags());

        List<Comment> comments = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), Comment.Status.VISIBLE);
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
                commentResponses, savedTags);
    }

    @Override
    @Transactional
    public void deletePost(String id, String userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // V24 FK 없음 — 앱 레이어에서 cascade 처리
        postHashtagRepository.deleteAllByPostId(id);
        post.delete();
    }

    @Override
    @Transactional
    public void toggleLike(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (post.getStatus() == Post.Status.DELETED) {
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

    @Override
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> getTrending(int days, int limit) {
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(days);
        List<Post> posts = postRepository.findTrending(since, PageRequest.of(0, limit));

        Set<String> userIds = posts.stream()
                .filter(p -> !p.isAnonymous())
                .map(Post::getUserId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, String> nicknameMap = userNicknameResolver.loadNicknames(userIds);

        List<String> postIds = posts.stream().map(Post::getId).toList();
        Map<String, Long> commentCountMap = commentCountMap(postIds);

        // 태그도 배치 조회
        Map<String, List<String>> tagMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : postHashtagRepository.findByPostIdIn(postIds).stream()
                        .collect(Collectors.groupingBy(
                                PostHashtag::getPostId,
                                Collectors.mapping(PostHashtag::getTag, Collectors.toList())));

        return posts.stream()
                .map(post -> {
                    int commentCount = commentCountMap.getOrDefault(post.getId(), 0L).intValue();
                    String nickname = userNicknameResolver.resolve(nicknameMap, post.getUserId());
                    List<String> tags = tagMap.getOrDefault(post.getId(), List.of());
                    return PostSummaryResponse.of(post, commentCount, nickname, tags);
                })
                .toList();
    }

    /**
     * 피드 정렬 옵션. 알 수 없는 값은 latest 로 fallback — 400 을 내지 않아 프론트 캐시/북마크에 강하다.
     * 동순위는 createdAt DESC 로 결정.
     */
    private Sort resolveSort(String sort) {
        String normalized = sort == null ? "" : sort.trim().toLowerCase();
        return switch (normalized) {
            case "popular" -> Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt"));
            case "views" -> Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("createdAt"));
            default -> Sort.by(Sort.Order.desc("createdAt"));
        };
    }

    /**
     * 태그 목록을 중복 제거 후 post_hashtags 에 저장하고 저장된 태그 문자열 리스트를 반환한다.
     * tags 가 null 이거나 비어 있으면 아무것도 저장하지 않고 빈 리스트를 반환한다.
     */
    private List<String> saveHashtags(String postId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        // 입력 순서를 유지하면서 중복 제거
        List<String> deduped = new java.util.ArrayList<>(new LinkedHashSet<>(tags));
        List<PostHashtag> entities = deduped.stream()
                .map(t -> PostHashtag.builder().postId(postId).tag(t).build())
                .toList();
        postHashtagRepository.saveAll(entities);
        return deduped;
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
