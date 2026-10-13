package com.group6.electronic_drafting_system.dto.Article;

import java.util.List;

// Trang "Bài viết của tôi": thông tin phóng viên cho topbar, nhiệm vụ được viết bài và các bài đã viết.
public record ArticleListPageDTO(String fullName, String departmentName, String initials,
                                 List<AssignedTopicDTO> assignments, List<ArticleListItemDTO> articles) {
}
