package com.group6.electronic_drafting_system.dto.Publication;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

// Một dòng trong Approved Article List: bài APPROVED, chờ Thư ký quyết định xuất bản
@Value @Builder
public class ApprovedArticleItemDTO {
    Integer articleId;
    String title;
    String topicTitle;
    String category;
    String leadName;
    // Phiên bản đã được Thư ký duyệt cuối (bản sẽ được đăng)
    String versionLabel;
    String finalApprovedBy;
    OffsetDateTime finalApprovedAt;
}
