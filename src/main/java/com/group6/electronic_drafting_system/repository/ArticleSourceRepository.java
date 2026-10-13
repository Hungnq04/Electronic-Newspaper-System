package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleSource;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleSourceRepository extends JpaRepository<ArticleSource, Integer> {
    @EntityGraph(attributePaths = "source")
    List<ArticleSource> findByArticle(Article article);
}
