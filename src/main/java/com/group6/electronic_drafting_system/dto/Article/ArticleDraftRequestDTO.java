package com.group6.electronic_drafting_system.dto.Article;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Dữ liệu trình soạn thảo gửi lên khi Lưu nháp / Nộp bài. content là HTML, được lọc lại ở service.
@Getter @Setter
public class ArticleDraftRequestDTO {
    @Size(max = 255) private String title;
    @Size(max = 255) private String subtitle;
    private String sapo;
    private String content;
    @Size(max = 100) private String category;
    // Chỉ dùng khi nộp bài: ghi vào article_version.change_summary
    private String changeSummary;
}
