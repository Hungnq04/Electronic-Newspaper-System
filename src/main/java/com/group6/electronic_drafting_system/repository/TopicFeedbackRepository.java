package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.TopicFeedback;
import com.group6.electronic_drafting_system.entity.enums.TopicFeedbackType;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicFeedbackRepository extends JpaRepository<TopicFeedback, Integer> {
    // Các vòng yêu cầu sửa của Trưởng ban, cũ nhất trước
    @EntityGraph(attributePaths = "author")
    List<TopicFeedback> findByTopic_IdOrderByCreatedAtAscIdAsc(Integer topicId);

    // Số vòng Trưởng ban đã yêu cầu sửa
    long countByTopic_IdAndType(Integer topicId, TopicFeedbackType type);
}
