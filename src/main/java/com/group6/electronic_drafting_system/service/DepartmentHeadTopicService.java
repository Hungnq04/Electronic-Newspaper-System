package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ChangeLeadFormDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DepartmentHeadTopicBoardDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentFormDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentPageDTO;

public interface DepartmentHeadTopicService {
    DepartmentHeadTopicBoardDTO getBoard(String email);
    TopicAssignmentPageDTO getAssignmentPage(Integer topicId, String email);
    void confirmAssignment(Integer topicId, TopicAssignmentFormDTO form, String email);
    void addSupporters(Integer topicId, TopicAssignmentFormDTO form, String email);
    void removeSupporter(Integer topicId, Integer assignmentId, String email);
    void changeLead(Integer topicId, ChangeLeadFormDTO form, String email);
}
