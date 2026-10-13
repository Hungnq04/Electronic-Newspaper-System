package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Article.ArticleCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleDraftRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleEditorDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleListItemDTO;
import com.group6.electronic_drafting_system.dto.Article.AssignedTopicDTO;
import java.time.OffsetDateTime;
import java.util.List;

public interface ArticleService {
    List<ArticleListItemDTO> getMyArticles(String email);
    List<AssignedTopicDTO> getTopicsWaitingForArticle(String email);
    ArticleEditorDTO getNewArticleForm(ArticleCreateRequestDTO form, String email);
    Integer create(ArticleCreateRequestDTO request, boolean submit, String email);
    ArticleEditorDTO getEditor(Integer articleId, String email);
    OffsetDateTime saveDraft(Integer articleId, ArticleDraftRequestDTO request, String email);
    void submit(Integer articleId, ArticleDraftRequestDTO request, String email);
    void setFeaturedImage(Integer articleId, Integer articleSourceId, String email);
}
