package com.group6.electronic_drafting_system.service;

import java.util.List;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewCommentDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;

public interface DepartmentHeadReviewService {
    VersionViewDTO openArticle(String email, Integer articleId);
    void approve(String email, Integer articleId, String feedback);
    void requestRevision(String email, Integer articleId, String feedback);
    void addComment(String email, Integer articleId, String content,
                String anchorField, Integer anchorParagraph, Integer anchorStart, Integer anchorEnd);
    List<ReviewCommentDTO> getComments(String email, Integer articleId);
}