package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Topic.EditorialTopicForm;
import com.group6.electronic_drafting_system.dto.Topic.EditorialTopicRowDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicAssignForm;
import com.group6.electronic_drafting_system.dto.Topic.TopicAssignPageDTO;
import java.util.List;

// Topic cấp tòa soạn của Thư ký (FL-02: UC-05 Create Editorial Topic, UC-06 Assign, UC-07 Reassign)
public interface EditorialTopicService {
    // Topic ACTIVE chưa có Trưởng phòng phụ trách ("Topic chưa giao")
    List<EditorialTopicRowDTO> getUnassignedTopics(String secretaryEmail);

    // Topic ACTIVE đã có Trưởng phòng phụ trách
    List<EditorialTopicRowDTO> getAssignedTopics(String secretaryEmail);

    List<String> getCategorySuggestions();

    // Tạo Topic ở trạng thái ACTIVE, trả về id
    Integer createTopic(EditorialTopicForm form, String secretaryEmail);

    TopicAssignPageDTO getAssignPage(Integer topicId, String secretaryEmail);

    // Giao Topic cho Trưởng phòng; nếu đã có người phụ trách thì giao lại (Assignment cũ chuyển REMOVED)
    void assignDepartmentHead(Integer topicId, TopicAssignForm form, String secretaryEmail);
}
