package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Topic.DepartmentOptionDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicProposalListDTO;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.util.List;

public interface TopicService {
    // statusFilter == null: tất cả. Phạm vi Topic được xem tùy vai trò của người đăng nhập.
    TopicProposalListDTO getProposalList(String email, TopicStatus statusFilter);
    List<DepartmentOptionDTO> getDepartmentOptions();
    Integer createProposal(String email, TopicCreateRequestDTO request);
}
