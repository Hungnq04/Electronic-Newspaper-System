package com.group6.electronic_drafting_system.dto.Reporter;

import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import java.time.OffsetDateTime;

// Một dòng trong danh sách nhiệm vụ của phóng viên. Chỉ chứa dữ liệu, mọi giá trị đã được
// ReporterTaskServiceImpl tính sẵn.
// lead: true = người dùng là viết chính (LEAD) của nhiệm vụ này
// deadlineText: "Còn 45 phút" / "Quá hạn 2 giờ" / "Nộp đúng hạn"...; deadlineBucket: overdue / today / week / later / none (lọc theo hạn)
// sourceCount: tổng số tư liệu = imageCount + documentCount + otherSourceCount
// writeHref: link mở trình soạn thảo (null nếu không phải viết chính); newArticleHref: link viết bài mới (null nếu không được viết)
// writeLabel: chữ trên nút viết bài ("Bắt đầu viết", "Tiếp tục viết"...)
public record ReporterTaskDTO(Integer assignmentId, Integer topicId, String topicTitle, String category,
                              Priority priority, String priorityLabel, AssignmentRole role, String roleLabel, boolean lead,
                              String assignedByName, String assignedByInitials, OffsetDateTime assignedAt, OffsetDateTime deadline,
                              String deadlineText, String deadlineBucket, String requirements,
                              Integer articleId, String articleTitle, ArticleStatus articleStatus, OffsetDateTime articleUpdatedAt,
                              int imageCount, int documentCount, int otherSourceCount, int sourceCount,
                              TaskStage stage, boolean urgent, boolean overdue, boolean canCreateArticle,
                              String writeHref, String newArticleHref, String writeLabel) {
}
