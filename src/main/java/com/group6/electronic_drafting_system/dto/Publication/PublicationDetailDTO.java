package com.group6.electronic_drafting_system.dto.Publication;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

// Dữ liệu màn Publication Detail: nội dung đúng phiên bản sẽ đăng + thông tin duyệt + lịch đăng (nếu có)
@Value @Builder
public class PublicationDetailDTO {
    Integer articleId;
    String status;          // tên trạng thái trong DB, vd APPROVED
    String statusLabel;     // tên tiếng Việt để hiển thị
    String versionLabel;

    // Nội dung lấy từ article_version (bản chụp đã duyệt), không lấy bản đang soạn
    String title;
    String subtitle;
    String sapo;
    List<String> paragraphs;
    String category;
    String featuredImageUrl;
    String featuredImageCaption;

    String topicTitle;
    String leadName;
    String departmentApprovedBy;
    OffsetDateTime departmentApprovedAt;
    String finalApprovedBy;
    OffsetDateTime finalApprovedAt;
    String finalFeedback;

    // Lịch đăng / thời điểm đăng (null nếu bài chưa có publication)
    String publicationTypeLabel;
    OffsetDateTime scheduledAt;
    OffsetDateTime publishedAt;
    String publishedBy;

    // true khi bài đang APPROVED: hiện nút Đăng ngay / Hẹn giờ
    boolean canPublish;
    // Giá trị nhỏ nhất cho ô chọn giờ (định dạng yyyy-MM-ddTHH:mm, giờ Việt Nam)
    String minScheduleAt;
}
