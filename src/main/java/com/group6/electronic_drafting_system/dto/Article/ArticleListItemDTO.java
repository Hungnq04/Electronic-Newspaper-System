package com.group6.electronic_drafting_system.dto.Article;

import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import java.time.OffsetDateTime;

// Một dòng trong trang "Bài viết của tôi". Chỉ chứa dữ liệu, nhãn đã được ArticleServiceImpl tính sẵn.
// statusGroup: nhóm màu giống bảng nhiệm vụ (doing / review / done)
public record ArticleListItemDTO(Integer id, String title, String topicTitle, String category,
                                 ArticleStatus status, String statusLabel, String statusGroup, OffsetDateTime updatedAt) {
}
