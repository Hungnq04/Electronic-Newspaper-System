package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PendingArticleDTO {
    private Integer articleId;     // bước 8 sẽ dùng để tạo link
    private String title;
}