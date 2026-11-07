package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Topic.DepartmentOptionDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicFeedbackViewDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicProposalListDTO;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;

public interface TopicService {
    // statusFilter == null: tất cả. Phạm vi Topic được xem tùy vai trò của người đăng nhập.
    TopicProposalListDTO getProposalList(String email, TopicStatus statusFilter);
    // Ban của phóng viên đang đăng nhập (đề tài mới luôn gửi về ban này); null nếu chưa được gán ban
    DepartmentOptionDTO getMyDepartment(String email);
    Integer createProposal(String email, TopicCreateRequestDTO request);

    // FL-01 Iter2 – Topic Feedback Viewer: chỉ chủ đề tài được xem
    TopicFeedbackViewDTO getFeedbackView(String email, Integer topicId);
    // FL-01 Iter2 – Edit & Resubmit Topic: chỉ chủ đề tài, chỉ khi DEPARTMENT_REVIEW + awaiting_revision
    TopicCreateRequestDTO getEditForm(String email, Integer topicId);
    void resubmit(String email, Integer topicId, TopicCreateRequestDTO request);
}
