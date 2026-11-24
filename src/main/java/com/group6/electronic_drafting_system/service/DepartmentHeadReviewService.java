package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewWorkspaceDTO;

public interface DepartmentHeadReviewService {
    ReviewWorkspaceDTO openWorkspace(String email, Integer articleId);
    void approve(String email, Integer articleId, String feedback);
    void requestRevision(String email, Integer articleId, String feedback);
    void addComment(String email, Integer articleId, String content);
}   