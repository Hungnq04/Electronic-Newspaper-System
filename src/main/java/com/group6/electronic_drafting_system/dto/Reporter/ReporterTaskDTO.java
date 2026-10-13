package com.group6.electronic_drafting_system.dto.Reporter;

import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import java.time.OffsetDateTime;

// Một dòng trong danh sách nhiệm vụ của phóng viên.
// deadlineText: "Còn 45 phút" / "Quá hạn 2 giờ" / "Nộp đúng hạn"...; deadlineBucket: overdue / today / week / later / none (lọc theo hạn)
public record ReporterTaskDTO(Integer assignmentId, Integer topicId, String topicTitle, String category,
                              Priority priority, String priorityLabel, AssignmentRole role, String roleLabel,
                              String assignedByName, String assignedByInitials, OffsetDateTime assignedAt, OffsetDateTime deadline,
                              String deadlineText, String deadlineBucket, String requirements,
                              Integer articleId, String articleTitle, ArticleStatus articleStatus, OffsetDateTime articleUpdatedAt,
                              int imageCount, int documentCount, int otherSourceCount,
                              TaskStage stage, boolean urgent, boolean overdue, boolean canCreateArticle) {

    public boolean lead() { return role == AssignmentRole.LEAD; }

    // Chỉ người viết chính mới mở trình soạn thảo được; bài mới luôn tạo từ chính assignment này
    public String writeHref() {
        if (!lead()) return null;
        return articleId == null ? newArticleHref() : "/articles/" + articleId + "/edit";
    }

    // Một đề tài có nhiều bài (BR-01): nhiệm vụ viết chính còn hiệu lực luôn viết thêm bài được
    public String newArticleHref() {
        return canCreateArticle ? "/articles/new?assignmentId=" + assignmentId : null;
    }

    public String writeLabel() {
        return switch (stage) {
            case NOT_STARTED -> "Bắt đầu viết";
            case IN_PROGRESS -> "Tiếp tục viết";
            case REVISION -> "Sửa bài";
            case IN_REVIEW, DONE -> "Xem bài";
        };
    }

    public int sourceCount() { return imageCount + documentCount + otherSourceCount; }
}
