package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Article.ArticleCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleDraftRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleEditorDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleListPageDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleMediaUploadResponseDTO;
import java.time.OffsetDateTime;
import org.springframework.web.multipart.MultipartFile;

public interface ArticleService {
    ArticleListPageDTO getListPage(String email);
    ArticleEditorDTO getNewArticleForm(ArticleCreateRequestDTO form, String email);
    Integer create(ArticleCreateRequestDTO request, boolean submit, String email);
    ArticleEditorDTO getEditor(Integer articleId, String email);
    OffsetDateTime saveDraft(Integer articleId, ArticleDraftRequestDTO request, String email);
    void submit(Integer articleId, ArticleDraftRequestDTO request, String email);
    void setFeaturedImage(Integer articleId, Integer articleSourceId, String email);
    ArticleMediaUploadResponseDTO attachTopicMedia(Integer articleId, Integer sourceId, String email);
    ArticleMediaUploadResponseDTO uploadMedia(Integer articleId, MultipartFile file, String caption, String email);
    String restoreVersion(Integer articleId, Integer versionId, String email);
}
