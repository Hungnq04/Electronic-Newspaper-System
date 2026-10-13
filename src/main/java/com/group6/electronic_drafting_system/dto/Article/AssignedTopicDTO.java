package com.group6.electronic_drafting_system.dto.Article;

import java.time.OffsetDateTime;

// Nhiệm vụ người dùng là LEAD (còn ASSIGNED, đề tài ACTIVE): được viết bài mới từ nhiệm vụ này.
// Một đề tài có thể có nhiều bài (BR-01), articleCount là số bài người dùng đã viết cho đề tài.
public record AssignedTopicDTO(Integer assignmentId, Integer topicId, String title, String category,
                               OffsetDateTime deadline, long articleCount) {
}
