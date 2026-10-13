package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.ArticleVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Integer> {
    // Mọi version của bài, cũ trước mới sau (1.0, 1.1, 1.2...)
    List<ArticleVersion> findByArticleIdOrderByVersionNumberAsc(Integer articleId);
}
