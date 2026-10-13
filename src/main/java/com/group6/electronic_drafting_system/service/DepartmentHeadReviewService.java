package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.DepartmentHead.CommentRequestDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewWorkspaceDTO;

// Trưởng phòng thẩm định một bài viết (FL-05): xem, góp ý, duyệt hoặc trả bài cho Lead
public interface DepartmentHeadReviewService {
    ReviewWorkspaceDTO openWorkspace(String email, Integer articleId);

    void addComment(String email, Integer articleId, CommentRequestDTO request);

    void deleteComment(String email, Integer articleId, Integer commentId);

    void setPreviousCommentResolved(String email, Integer articleId, Integer commentId, boolean resolved);

    void approve(String email, Integer articleId, String feedback);

    void requestRevision(String email, Integer articleId, String feedback);
}
