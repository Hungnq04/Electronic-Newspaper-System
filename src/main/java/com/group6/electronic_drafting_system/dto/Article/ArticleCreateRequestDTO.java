package com.group6.electronic_drafting_system.dto.Article;

import lombok.Getter;
import lombok.Setter;

// Tạo bài mới: bài luôn xuất phát từ một assignment (viết chính) của người dùng; topic lấy theo assignment đó.
@Getter @Setter
public class ArticleCreateRequestDTO extends ArticleDraftRequestDTO {
    private Integer assignmentId;
}
