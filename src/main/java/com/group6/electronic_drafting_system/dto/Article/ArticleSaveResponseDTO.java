package com.group6.electronic_drafting_system.dto.Article;

import java.time.OffsetDateTime;

public record ArticleSaveResponseDTO(boolean success, String message, OffsetDateTime updatedAt) {
}
