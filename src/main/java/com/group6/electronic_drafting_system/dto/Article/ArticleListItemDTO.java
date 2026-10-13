package com.group6.electronic_drafting_system.dto.Article;

import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import java.time.OffsetDateTime;

public record ArticleListItemDTO(Integer id, String title, String topicTitle, String category,
                                 ArticleStatus status, OffsetDateTime updatedAt) {
}
