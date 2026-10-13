package com.group6.electronic_drafting_system.dto.Article;

import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import java.time.OffsetDateTime;

public record ArticleListItemDTO(Integer id, String title, String topicTitle, String category,
                                 ArticleStatus status, OffsetDateTime updatedAt) {

    public String statusLabel() { return ArticleEditorDTO.labelOf(status); }

    // Nhóm màu giống bảng nhiệm vụ: todo / doing / review / done
    public String statusGroup() {
        return switch (status) {
            case DRAFT, REVISION_REQUIRED -> "doing";
            case DEPARTMENT_REVIEW, DEPARTMENT_APPROVED -> "review";
            case APPROVED, SCHEDULED, PUBLISHED -> "done";
        };
    }
}
