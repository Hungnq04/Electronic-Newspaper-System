package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleReviewRepository extends JpaRepository<ArticleReview, Integer> {
    @Query("select count(r) from ArticleReview r where r.reviewStage = :stage and r.decision = :decision "
            + "and r.reviewedAt >= :from and r.reviewer.id = :reviewerId")
    long countDecisionsSince(@Param("stage") ReviewStage stage, @Param("decision") ArticleReviewDecision decision,
                             @Param("from") OffsetDateTime from, @Param("reviewerId") Integer reviewerId);

    // Lượt duyệt đang mở (decision = NULL) của một cấp trên một version; DB chỉ cho phép tối đa 1 dòng (uq_review_open)
    @Query("select r from ArticleReview r join fetch r.reviewer where r.version.id = :versionId "
            + "and r.reviewStage = :stage and r.decision is null")
    Optional<ArticleReview> findOpenReview(@Param("versionId") Integer versionId, @Param("stage") ReviewStage stage);
}
