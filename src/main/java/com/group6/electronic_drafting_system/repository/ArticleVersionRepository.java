package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.ArticleVersion;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Integer> {
    @EntityGraph(attributePaths = "creator")
    List<ArticleVersion> findByArticle_IdOrderByVersionNumberDesc(Integer articleId);
    @Query("select coalesce(max(v.versionNumber), 0) from ArticleVersion v where v.article.id = :articleId")
    int findMaxVersionNumber(@Param("articleId") Integer articleId);
}
