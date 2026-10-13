package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.ArticleReview;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleReviewRepository extends JpaRepository<ArticleReview, Integer> {
    @EntityGraph(attributePaths = "reviewer")
    Optional<ArticleReview> findFirstByVersion_Article_IdOrderByIdDesc(Integer articleId);
}
