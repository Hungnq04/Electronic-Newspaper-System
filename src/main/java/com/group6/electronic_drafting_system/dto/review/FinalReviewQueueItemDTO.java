package com.group6.electronic_drafting_system.dto.review;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

// Một dòng trong hàng đợi duyệt cuối của Thư ký (bài DEPARTMENT_APPROVED)
@Value @Builder
public class FinalReviewQueueItemDTO {
    Integer articleId;
    String title;
    String topicTitle;
    String category;
    String priority;
    String leadName;
    String versionLabel;
    OffsetDateTime submittedAt;
    String departmentApprovedBy;
    OffsetDateTime departmentApprovedAt;
    // Thư ký đang mở lượt duyệt (null nếu chưa ai mở)
    String reviewingBy;
    boolean reviewingByMe;
    // BR-34: Thư ký là Lead của bài thì không được duyệt
    boolean ownArticle;
}
