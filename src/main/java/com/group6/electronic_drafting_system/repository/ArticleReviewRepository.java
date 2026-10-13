package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleReviewRepository extends JpaRepository<ArticleReview, Integer> {
    // Lượt đang mở (chưa quyết định) của một cấp trên một version - DB chỉ cho phép tối đa một
    @EntityGraph(attributePaths = "reviewer")
    Optional<ArticleReview> findByVersionAndReviewStageAndDecisionIsNull(ArticleVersion version, ReviewStage stage);

    boolean existsByVersionAndReviewStageAndDecision(ArticleVersion version, ReviewStage stage, ArticleReviewDecision decision);

    @EntityGraph(attributePaths = {"reviewer", "version"})
    List<ArticleReview> findByVersionIn(Collection<ArticleVersion> versions);

    @EntityGraph(attributePaths = {"reviewer", "version"})
    List<ArticleReview> findByVersionArticleAndDecisionIsNotNullOrderByReviewedAtAscIdAsc(Article article);
}
