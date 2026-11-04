package com.group6.electronic_drafting_system.dto.Topic;

import java.time.OffsetDateTime;

// Một dòng lịch sử góp ý / kết quả duyệt của đề tài (từ topic_feedback hoặc topic_review)
// tone: revision (yêu cầu sửa), approved (đã duyệt), rejected (từ chối) – dùng để tô màu badge
public record TopicHistoryEntryDTO(String label, String tone, String authorName, OffsetDateTime createdAt, String content) {}
