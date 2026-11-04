package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// Toàn bộ dữ liệu của màn "Thẩm định bản thảo" (department_head/workspacereview.html)
@Getter
@Builder
public class ReviewWorkspaceDTO {
    private Integer articleId;
    private String code;
    private String topicTitle;
    private String category;
    private String statusText;
    // true = bài đang DEPARTMENT_REVIEW và lượt duyệt đang mở là của người đang xem: được góp ý / duyệt / trả bài
    private boolean reviewable;
    private String reviewerName;
    private String reviewerInitials;
    private String departmentName;

    private String leadName;
    private String leadInitials;
    private String supportingNames;
    private String wordCount;
    private int imageCount;

    // Version đang duyệt (mới nhất), version liền trước (null nếu mới có v1.0) và toàn bộ version để chọn so sánh
    private VersionViewDTO current;
    private VersionViewDTO previous;
    private List<VersionViewDTO> versions;

    private List<SourceItemDTO> sources;
    // Góp ý của lượt duyệt trên version đang duyệt / góp ý trên version liền trước
    private List<ReviewCommentDTO> comments;
    private List<ReviewCommentDTO> previousComments;
}
