package com.gout.service;

import com.gout.dto.request.CreateCommentRequest;
import com.gout.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    List<CommentResponse> getComments(String postId);

    CommentResponse createComment(String postId, String userId, CreateCommentRequest request);

    void deleteComment(String id, String userId);
}
