package com.group6.electronic_drafting_system.dto.Article;

import lombok.Getter;
import lombok.Setter;

// Tạo bài mới: thêm đề tài (article.topic_id bắt buộc) vào dữ liệu soạn thảo.
@Getter @Setter
public class ArticleCreateRequestDTO extends ArticleDraftRequestDTO {
    private Integer topicId;
}
