package com.group6.electronic_drafting_system.dto.Article;

import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
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
        Review latestReview, List<Version> versions, List<Source> sources) {

    public record Version(Integer id, Integer versionNumber, String versionLabel, String title,
                          String changeSummary, String creatorName, OffsetDateTime createdAt) {}

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
