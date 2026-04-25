package com.gout.controller;

import com.gout.dto.request.CreateCommentRequest;
import com.gout.dto.request.EditCommentRequest;
import com.gout.dto.response.CommentResponse;
import com.gout.global.response.ApiResponse;
import com.gout.security.CurrentUserProvider;
import com.gout.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> list(@PathVariable String postId) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(postId)));
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @PathVariable String postId,
            @Valid @RequestBody CreateCommentRequest request) {
        String userId = currentUserProvider.requireUserId();
        return ResponseEntity.ok(
                ApiResponse.success("댓글이 작성되었습니다.",
                        commentService.createComment(postId, userId, request)));
    }

    @PutMapping("/api/comments/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> edit(
            @PathVariable String id,
            @Valid @RequestBody EditCommentRequest request) {
        String userId = currentUserProvider.requireUserId();
        CommentResponse updated = commentService.editComment(id, userId, request.getContent());
        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다.", updated));
    }

    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        String userId = currentUserProvider.requireUserId();
        commentService.deleteComment(id, userId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다.", null));
    }

}
