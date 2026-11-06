package com.group6.electronic_drafting_system.dto.Topic;

import java.time.OffsetDateTime;

// Một thông báo trong danh sách của chuông
public record NotificationItemDTO(Integer id, Integer topicId, String message, boolean read, OffsetDateTime createdAt) {}
