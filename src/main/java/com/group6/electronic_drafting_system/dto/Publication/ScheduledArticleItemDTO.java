package com.group6.electronic_drafting_system.dto.Publication;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

// Một dòng trong Scheduled Publication List: bài SCHEDULED, đang chờ đến giờ để hệ thống tự đăng
@Value @Builder
public class ScheduledArticleItemDTO {
    Integer articleId;
    String title;
    String topicTitle;
    String category;
    String leadName;
    // Phiên bản được ghim trong publication (bản sẽ được đăng)
    String versionLabel;
    OffsetDateTime scheduledAt;
    // Thư ký đã đặt lịch
    String scheduledBy;
    // true khi đã đến giờ nhưng job chưa kịp đăng (job chạy mỗi phút): không cho hủy nữa
    boolean due;
}
