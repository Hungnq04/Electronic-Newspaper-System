package com.group6.electronic_drafting_system.dto.Article;

import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.CommentStatus;
import com.group6.electronic_drafting_system.entity.enums.Priority;
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
        Review latestReview, List<Version> versions, List<Source> sources, List<TopicMedia> topicMedia) {

    // Bản chụp khi nộp kèm các lượt duyệt và góp ý (comment) trên chính phiên bản đó
    public record Version(Integer id, Integer versionNumber, String versionLabel, String title, String subtitle, String sapo, String content,
                          String changeSummary, String creatorName, OffsetDateTime createdAt,
                          List<VersionReview> reviews, List<VersionComment> comments) {
        public String label() { return versionLabel != null ? versionLabel : String.valueOf(versionNumber); }

        public boolean hasRevisionRequest() {
            return reviews.stream().anyMatch(r -> r.decision() == ArticleReviewDecision.REQUEST_REVISION);
        }
    }

    public record VersionReview(String reviewerName, String stageLabel, ArticleReviewDecision decision, String feedback, OffsetDateTime reviewedAt) {
        public String decisionLabel() {
            if (decision == null) return "Đang duyệt";
            return decision == ArticleReviewDecision.APPROVED ? "Đã duyệt" : "Yêu cầu chỉnh sửa";
        }
    }

    public record VersionComment(String anchorLabel, String quotedText, String content, String authorName, String stageLabel,
                                 CommentStatus status, OffsetDateTime createdAt, List<VersionComment> replies) {
        public boolean isOpen() { return status == CommentStatus.OPEN; }
    }

    // Ảnh / video trong tư liệu của đề tài (topic_source), do phóng viên hỗ trợ hoặc viết chính tải lên
    public record TopicMedia(Integer sourceId, String title, String fileUrl, boolean video, String note, String addedByName) {
        public String caption() { return note != null ? note : title; }
    }

    public record Review(String reviewerName, ArticleReviewDecision decision, String feedback, OffsetDateTime reviewedAt) {}

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
