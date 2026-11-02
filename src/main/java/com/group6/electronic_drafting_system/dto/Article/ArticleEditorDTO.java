package com.group6.electronic_drafting_system.dto.Article;

import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.CommentStatus;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import com.group6.electronic_drafting_system.mapper.Reporter.ReporterTaskMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// Toàn bộ dữ liệu trang soạn bài cần, lấy xong trong transaction để template không phải lazy-load.
public record ArticleEditorDTO(
        Integer id, String title, String subtitle, String sapo, String content, String category,
        ArticleStatus status, boolean editable,
        OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime submittedAt,
        Integer assignmentId, Integer topicId, String topicTitle, String topicCategory, Priority topicPriority, LocalDate topicExpectedDate,
        String leadName, String departmentName, String reviewerName, OffsetDateTime deadline,
        Review latestReview, List<Version> versions,
        // Phiên bản vừa bị trả về (chỉ có khi bài đang REVISION_REQUIRED), dùng cho thẻ "Góp ý cần sửa"; null nếu không bị trả về
        Version returnedVersion,
        List<Source> sources, List<TopicMedia> topicMedia) {

    // Bản chụp khi nộp kèm các lượt duyệt và góp ý (comment) trên chính phiên bản đó.
    // view: nội dung đã chia thành từng đoạn GIỐNG HỆT màn Trưởng ban, để tô vàng đúng vị trí góp ý
    public record Version(Integer id, Integer versionNumber, String versionLabel, String title, String subtitle, String sapo, String content,
                          String changeSummary, String creatorName, OffsetDateTime createdAt,
                          List<VersionReview> reviews, List<VersionComment> comments, VersionViewDTO view) {
        public String label() { return versionLabel != null ? versionLabel : String.valueOf(versionNumber); }

        public boolean hasRevisionRequest() {
            return reviews.stream().anyMatch(r -> r.decision() == ArticleReviewDecision.REQUEST_REVISION);
        }

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
    public record VersionReview(String reviewerName, String stageLabel, ReviewStage stage, ArticleReviewDecision decision,
                                String feedback, OffsetDateTime reviewedAt) {
        public String decisionLabel() {
            if (decision == null) return "Đang duyệt";
            return decision == ArticleReviewDecision.APPROVED ? "Đã duyệt" : "Yêu cầu chỉnh sửa";
        }

        // Tên class CSS để tô màu nhãn vai trò: "department" hoặc "secretary"
        public String roleClass() { return roleClassOf(stage); }
    }

    // anchorField / anchorParagraph / anchorStart / anchorEnd: vị trí góp ý trong phiên bản (null = góp ý chung, không gắn vị trí)
    public record VersionComment(Integer id, String anchorLabel, AnchorField anchorField, Integer anchorParagraph,
                                 Integer anchorStart, Integer anchorEnd, String quotedText, String content,
                                 String authorName, String stageLabel, ReviewStage stage,
                                 CommentStatus status, OffsetDateTime createdAt, List<VersionComment> replies) {
        public boolean isOpen() { return status == CommentStatus.OPEN; }

        public boolean hasAnchor() { return anchorField != null; }

        public String roleClass() { return roleClassOf(stage); }
    }

    private static String roleClassOf(ReviewStage stage) {
        return stage == ReviewStage.SECRETARY ? "secretary" : "department";
    }

    // Ảnh / video trong tư liệu của đề tài (topic_source), do phóng viên hỗ trợ hoặc viết chính tải lên
    public record TopicMedia(Integer sourceId, String title, String fileUrl, boolean video, String note, String addedByName) {
        public String caption() { return note != null ? note : title; }
    }

    // Phản hồi gần nhất (đã có quyết định). stageLabel = "Trưởng ban" / "Thư ký tòa soạn"
    public record Review(String reviewerName, String stageLabel, ArticleReviewDecision decision, String feedback, OffsetDateTime reviewedAt) {}

    public record Source(Integer articleSourceId, String title, SourceType type, String fileUrl,
                         String caption, String note, boolean featured) {
        public boolean isImage() { return type == SourceType.IMAGE && fileUrl != null && !fileUrl.isBlank(); }
    }

    // Trang "Viết bài mới": bài chưa được lưu lần nào
    public boolean isNew() { return id == null; }

    public Source featuredImage() {
        return sources.stream().filter(s -> s.featured() && s.isImage()).findFirst().orElse(null);
    }

    public List<Source> imageSources() {
        return sources.stream().filter(Source::isImage).toList();
    }

    public String leadInitials() { return ReporterTaskMapper.initials(leadName); }

    public String statusLabel() { return labelOf(status); }

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

    public String priorityLabel() {
        if (topicPriority == null) return "";
        return switch (topicPriority) {
            case LOW -> "Thấp";
            case NORMAL -> "Bình thường";
            case HIGH -> "Cao";
            case URGENT -> "Khẩn";
        };
    }
}
