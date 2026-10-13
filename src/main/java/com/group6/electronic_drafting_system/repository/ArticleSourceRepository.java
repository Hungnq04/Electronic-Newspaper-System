package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.ArticleSource;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleSourceRepository extends JpaRepository<ArticleSource, Integer> {
    @EntityGraph(attributePaths = "source")
    @Query("select s from ArticleSource s where s.article.id = :articleId order by case when s.displayOrder is null then 1 else 0 end, s.displayOrder, s.attachedAt")
    List<ArticleSource> findByArticleOrdered(@Param("articleId") Integer articleId);
    @EntityGraph(attributePaths = "source")
    List<ArticleSource> findByArticle_IdIn(Collection<Integer> articleIds);
}
