package com.gout.service.impl;

import com.gout.dao.CommentRepository;
import com.gout.dao.PostRepository;
import com.gout.dto.request.CreateCommentRequest;
import com.gout.dto.response.CommentResponse;
import com.gout.entity.Comment;
import com.gout.entity.Post;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.CommentService;
import com.gout.service.NotificationService;
import com.gout.service.UserNicknameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    // V24 FK 제거 후 탈퇴 사용자 표기 통합 처리기.
    private final UserNicknameResolver userNicknameResolver;

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if ("DELETED".equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        List<Comment> comments = commentRepository
                .findByPostIdAndStatusOrderByCreatedAtAsc(postId, "VISIBLE");

        Set<String> userIds = comments.stream()
                .filter(c -> !c.isAnonymous())
                .map(Comment::getUserId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, String> nicknameMap = userNicknameResolver.loadNicknames(userIds);

        return comments.stream()
                .map(c -> CommentResponse.of(c,
                        userNicknameResolver.resolve(nicknameMap, c.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse createComment(String postId, String userId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if ("DELETED".equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
            if (!parent.getPostId().equals(postId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "부모 댓글이 다른 게시글에 속해 있습니다.");
            }
            if ("DELETED".equals(parent.getStatus())) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
            }
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .parentId(request.getParentId())
                .content(request.getContent())
                .isAnonymous(request.isAnonymous())
                .build();

        Comment saved = commentRepository.save(comment);
        String nickname = saved.isAnonymous() ? null : userNicknameResolver.resolve(userId);

        // === 알림 트리거 (Agent-G / feature/notifications) ===
        // 1) 게시글 작성자에게 COMMENT_ON_POST (자기 글에 자기 댓글은 제외)
        if (!post.getUserId().equals(userId)) {
            notificationService.createFor(
                    post.getUserId(),
                    "COMMENT_ON_POST",
                    "새 댓글이 달렸습니다",
                    saved.getContent(),
                    "/community/" + post.getId());
        }
        // 2) 부모 댓글 작성자에게 REPLY_ON_COMMENT (자기 댓글에 자기 답글은 제외, 게시글 작성자 중복 제외)
        if (saved.getParentId() != null) {
            commentRepository.findById(saved.getParentId()).ifPresent(parent -> {
                String parentAuthor = parent.getUserId();
                if (!parentAuthor.equals(userId) && !parentAuthor.equals(post.getUserId())) {
                    notificationService.createFor(
                            parentAuthor,
                            "REPLY_ON_COMMENT",
                            "댓글에 답글이 달렸습니다",
                            saved.getContent(),
                            "/community/" + post.getId());
                }
            });
        }

        return CommentResponse.of(saved, nickname);
    }

    @Override
    @Transactional
    public CommentResponse editComment(String id, String userId, String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // 삭제된 댓글은 조회 단계에서 "없는 것"으로 취급 (404)
        if ("DELETED".equals(comment.getStatus())) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 작성자 본인만 수정 가능 (403)
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        comment.edit(content);

        String nickname = comment.isAnonymous()
                ? null
                : userNicknameResolver.resolve(comment.getUserId());
        return CommentResponse.of(comment, nickname);
    }

    @Override
    @Transactional
    public void deleteComment(String id, String userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        comment.delete();
    }

}
