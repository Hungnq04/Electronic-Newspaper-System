package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Topic.TopicReviewWorkspaceDTO;

// FL-01 Iter2: Trưởng ban duyệt đề tài cấp ban, Thư ký duyệt cuối.
// Mỗi thao tác chạy trong một transaction, khóa dòng Topic và kiểm tra lại status trong DB.
public interface TopicReviewService {
    // Topic Review Workspace của Trưởng ban. selectedTopicId == null: chọn đề tài đầu hàng chờ
    TopicReviewWorkspaceDTO getHeadWorkspace(String email, Integer selectedTopicId);
    void headApprove(String email, Integer topicId);
    void headRequestChanges(String email, Integer topicId, String feedback);

    // Final Topic Review Workspace của Thư ký. departmentFilter == null: mọi ban
    TopicReviewWorkspaceDTO getSecretaryWorkspace(String email, Integer departmentFilter, Integer selectedTopicId);
    void secretaryApprove(String email, Integer topicId);
    void secretaryReject(String email, Integer topicId, String reason);
}
