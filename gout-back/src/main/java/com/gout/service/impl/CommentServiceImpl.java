package com.gout.service.impl;

import com.gout.dao.CommentRepository;
import com.gout.dao.PostRepository;
import com.gout.dao.UserRepository;
import com.gout.dto.request.CreateCommentRequest;
import com.gout.dto.response.CommentResponse;
import com.gout.entity.Comment;
import com.gout.entity.Post;
import com.gout.entity.User;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.CommentService;
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
    private final UserRepository userRepository;

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
        Map<String, String> nicknameMap = loadNicknames(userIds);

        return comments.stream()
                .map(c -> CommentResponse.of(c,
                        nicknameMap.getOrDefault(c.getUserId(), "알 수 없음")))
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
        String nickname = saved.isAnonymous() ? null : findNickname(userId);
        return CommentResponse.of(saved, nickname);
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
