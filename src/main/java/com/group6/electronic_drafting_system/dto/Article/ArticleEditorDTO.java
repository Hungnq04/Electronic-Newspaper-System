package com.group6.electronic_drafting_system.dto.Article;

import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.CommentStatus;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// Toàn bộ dữ liệu trang soạn bài cần, lấy xong trong transaction để template không phải lazy-load.
// Các giá trị hiển thị (nhãn, ảnh đại diện, danh sách ảnh...) đã được ArticleServiceImpl tính sẵn.
// isNew: true = trang "Viết bài mới", bài chưa được lưu lần nào
// featuredImage: ảnh đại diện của bài (null nếu chưa có); imageSources: các tư liệu là ảnh
public record ArticleEditorDTO(
        Integer id, String title, String subtitle, String sapo, String content, String category,
        ArticleStatus status, String statusLabel, boolean editable, boolean isNew,
        OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime submittedAt,
        Integer assignmentId, Integer topicId, String topicTitle, String topicCategory,
        Priority topicPriority, String priorityLabel, LocalDate topicExpectedDate,
        String leadName, String leadInitials, String departmentName, String reviewerName, OffsetDateTime deadline,
        Review latestReview, List<Version> versions,
        // Phiên bản vừa bị trả về (chỉ có khi bài đang REVISION_REQUIRED), dùng cho thẻ "Góp ý cần sửa"; null nếu không bị trả về
        Version returnedVersion,
        // Nhãn phiên bản vừa được khôi phục vào bản đang soạn (vd "1.0"); null nếu chưa khôi phục.
        // Dùng để điền sẵn ghi chú "Khôi phục từ version 1.0" ở lần nộp lại (UC-24)
        String restoredFromLabel,
        List<Source> sources, Source featuredImage, List<Source> imageSources,
        List<TopicMedia> topicMedia) {

    // Bản chụp khi nộp kèm các lượt duyệt và góp ý (comment) trên chính phiên bản đó.
    // label: nhãn hiển thị ("1.0", "1.1"...)
    // view: nội dung đã chia thành từng đoạn GIỐNG HỆT màn Trưởng ban, để tô vàng đúng vị trí góp ý
    public record Version(Integer id, Integer versionNumber, String versionLabel, String label,
                          String title, String subtitle, String sapo, String content,
                          String changeSummary, String creatorName, OffsetDateTime createdAt,
                          List<VersionReview> reviews, List<VersionComment> comments, VersionViewDTO view) {

        // Lượt duyệt đã trả bài về (lượt "Yêu cầu chỉnh sửa" gần nhất); null nếu phiên bản này chưa bị trả về
        public VersionReview returningReview() {
            VersionReview result = null;
            for (VersionReview review : reviews) {
                if (review.decision() == ArticleReviewDecision.REQUEST_REVISION) result = review;
            }
            return result;
        }
    }

    // stage: cấp duyệt (DEPARTMENT = Trưởng ban, SECRETARY = Thư ký tòa soạn); stageLabel là chữ hiển thị của cấp đó
    // decisionLabel: chữ hiển thị của quyết định ("Đang duyệt" / "Đã duyệt" / "Yêu cầu chỉnh sửa")
    public record VersionReview(String reviewerName, String stageLabel, ReviewStage stage, ArticleReviewDecision decision,
                                String decisionLabel, String feedback, OffsetDateTime reviewedAt) {

        // Tên class CSS để tô màu nhãn vai trò: "department" hoặc "secretary"
        public String roleClass() { return roleClassOf(stage); }
    }

    // anchorField / anchorParagraph / anchorStart / anchorEnd: vị trí góp ý trong phiên bản (null = góp ý chung, không gắn vị trí)
    public record VersionComment(Integer id, String anchorLabel, AnchorField anchorField, Integer anchorParagraph,
                                 Integer anchorStart, Integer anchorEnd, String quotedText, String content,
                                 String authorName, String stageLabel, ReviewStage stage,
                                 CommentStatus status, OffsetDateTime createdAt, List<VersionComment> replies) {

        public boolean hasAnchor() { return anchorField != null; }

        public String roleClass() { return roleClassOf(stage); }
    }

    private static String roleClassOf(ReviewStage stage) {
        return stage == ReviewStage.SECRETARY ? "secretary" : "department";
    }

    // Ảnh / video trong tư liệu của đề tài (topic_source), do phóng viên hỗ trợ hoặc viết chính tải lên
    // caption: chú thích hiển thị (ghi chú nếu có, không thì tên tư liệu)
    public record TopicMedia(Integer sourceId, String title, String fileUrl, boolean video, String note, String addedByName,
                             String caption) {
    }

    // Phản hồi gần nhất (đã có quyết định). stageLabel = "Trưởng ban" / "Thư ký tòa soạn"
    public record Review(String reviewerName, String stageLabel, ArticleReviewDecision decision, String feedback, OffsetDateTime reviewedAt) {}

    public record Source(Integer articleSourceId, String title, SourceType type, String fileUrl,
                         String caption, String note, boolean featured) {
    }

    // GIỮ LẠI vì DepartmentHeadServiceImpl (code của thành viên khác) đang gọi.
    // Code của mình dùng bản private statusLabel() trong ArticleServiceImpl.
    public static String labelOf(ArticleStatus status) {
        return switch (status) {
            case DRAFT -> "Bản thảo";
            case DEPARTMENT_REVIEW -> "Chờ trưởng ban duyệt";
            case REVISION_REQUIRED -> "Cần chỉnh sửa";
            case DEPARTMENT_APPROVED -> "Trưởng ban đã duyệt";
            case APPROVED -> "Đã duyệt xuất bản";
            case SCHEDULED -> "Đã lên lịch đăng";
            case PUBLISHED -> "Đã xuất bản";
        };
    }
}
