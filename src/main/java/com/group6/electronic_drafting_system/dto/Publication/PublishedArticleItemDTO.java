package com.group6.electronic_drafting_system.dto.Publication;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

// Một dòng trong danh sách "Đã xuất bản": bài PUBLISHED đang hiển thị trên trang công khai
@Value @Builder
public class PublishedArticleItemDTO {
    Integer articleId;
    String title;
    String topicTitle;
    String category;
    String leadName;
    // Phiên bản được ghim trong publication (bản độc giả đang đọc)
    String versionLabel;
    // "Đăng ngay" hoặc "Hẹn giờ"
    String publicationTypeLabel;
    OffsetDateTime publishedAt;
    // Thư ký đã bấm đăng / đặt lịch
    String publishedBy;
}
