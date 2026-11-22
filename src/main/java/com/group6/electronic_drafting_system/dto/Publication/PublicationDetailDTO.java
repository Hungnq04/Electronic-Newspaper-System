package com.group6.electronic_drafting_system.dto.Publication;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ContentBlockDTO;
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
    List<ContentBlockDTO> blocks;   // thân bài đã tách khối: đoạn chữ, ảnh, video, bảng
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

    // true khi bài đang APPROVED: hiện nút Đăng ngay / Hẹn giờ / Trả về Lead
    boolean canPublish;
    // Iter3: true khi bài đang SCHEDULED và chưa đến giờ đăng: hiện nút Hủy lịch & trả về Lead
    boolean canCancel;
    // Iter3: true khi bài SCHEDULED đã đến giờ nhưng job chưa kịp đăng
    boolean due;
    // Gỡ bài (thêm sau góp ý hội đồng): true khi bài đang PUBLISHED: hiện nút Gỡ bài
    boolean canUnpublish;
    // Giá trị nhỏ nhất cho ô chọn giờ (định dạng yyyy-MM-ddTHH:mm, giờ Việt Nam)
    String minScheduleAt;
}
