package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.TopicReview;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicReviewRepository extends JpaRepository<TopicReview, Integer> {
    // Kết quả duyệt ở một cấp của nhiều đề tài (mỗi đề tài tối đa một dòng / cấp), kèm người duyệt
    @EntityGraph(attributePaths = "reviewer")
    List<TopicReview> findByTopic_IdInAndReviewStage(Collection<Integer> topicIds, ReviewStage reviewStage);
}
