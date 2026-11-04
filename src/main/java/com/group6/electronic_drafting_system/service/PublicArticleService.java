package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.entity.PublicArticle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface PublicArticleService {
    int PAGE_SIZE = 9;

    List<PublicArticle> getLatest();

    // page bắt đầu từ 1 (đúng như trên URL)
    Page<PublicArticle> search(String keyword, String category, int page);

    Optional<PublicArticle> findPublished(int articleId);

    List<PublicArticle> getRelated(PublicArticle article);

    List<String> getCategories();
}
