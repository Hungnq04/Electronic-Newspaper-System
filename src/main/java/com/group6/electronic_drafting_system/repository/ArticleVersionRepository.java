package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Integer> {
    Optional<ArticleVersion> findFirstByArticleOrderByVersionNumberDesc(Article article);

    // Mọi version của bài, cũ trước mới sau (1.0, 1.1, 1.2...)
    List<ArticleVersion> findByArticleIdOrderByVersionNumberAsc(Integer articleId);

    // Version mới nhất của từng bài trong danh sách
    @Query("select v from ArticleVersion v where v.article in :articles and v.versionNumber = "
            + "(select max(v2.versionNumber) from ArticleVersion v2 where v2.article = v.article)")
    List<ArticleVersion> findLatestByArticleIn(@Param("articles") Collection<Article> articles);

    @EntityGraph(attributePaths = "creator")
    List<ArticleVersion> findByArticle_IdOrderByVersionNumberDesc(Integer articleId);
}
