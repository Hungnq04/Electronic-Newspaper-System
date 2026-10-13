package com.group6.electronic_drafting_system.dto.Article;

import java.time.OffsetDateTime;

// Đề tài người dùng là LEAD, đã ACTIVE nhưng chưa có bài viết.
public record AssignedTopicDTO(Integer topicId, String title, String category, OffsetDateTime deadline) {
}
