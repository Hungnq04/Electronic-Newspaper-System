package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.TopicReview;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicReviewRepository extends JpaRepository<TopicReview, Integer> {
    // Kết quả duyệt ở một cấp của nhiều đề tài (mỗi đề tài tối đa một dòng / cấp), kèm người duyệt
    @EntityGraph(attributePaths = "reviewer")
    List<TopicReview> findByTopic_IdInAndReviewStage(Collection<Integer> topicIds, ReviewStage reviewStage);

    // Mỗi Topic có tối đa một kết quả ở mỗi cấp (uq_topic_review_stage)
    @EntityGraph(attributePaths = "reviewer")
    Optional<TopicReview> findByTopic_IdAndReviewStage(Integer topicId, ReviewStage reviewStage);

    @EntityGraph(attributePaths = "reviewer")
    List<TopicReview> findByTopic_IdOrderByReviewedAtAscIdAsc(Integer topicId);
}
