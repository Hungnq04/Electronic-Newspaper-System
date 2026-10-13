package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    @EntityGraph(attributePaths = {"creator", "review", "review.version"})
    List<Comment> findByReviewVersionArticleOrderByCreatedAtAscIdAsc(Article article);
}
