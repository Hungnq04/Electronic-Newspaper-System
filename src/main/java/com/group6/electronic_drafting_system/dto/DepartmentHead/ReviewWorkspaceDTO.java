package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// Toàn bộ dữ liệu của màn "Thẩm định bản thảo" (department_head/workspacereview.html)
// Hộp lớn chứa các hộp nhỏ: VersionViewDTO (một phiên bản bài), ReviewCommentDTO (một góp ý)
@Getter
@Builder
public class ReviewWorkspaceDTO {
    private Integer articleId;                 // mã bài -> ghép URL các form POST
    private VersionViewDTO current;            // bản đang duyệt (mới nhất) -> ${workspace.current.title}...
    private VersionViewDTO previous;            // bản liền trước; null nếu bài mới nộp 1 lần
    private List<ReviewCommentDTO> comments;    // góp ý trên bản đang duyệt -> th:each="comment : ${workspace.comments}"

    private String code;               // mã hiển thị, ví dụ "BT-11"
    private String topicTitle;         // tên Topic
    private String category;           // chuyên mục
    private String statusText;         // trạng thái bằng chữ tiếng Việt
    private String reviewerName;       // tên Trưởng phòng đang xem
    private String reviewerInitials;   // chữ viết tắt trên avatar
    private String departmentName;     // tên phòng ban (có thể null)
    private String leadName;           // tên Lead
    private String leadInitials;       // chữ viết tắt của Lead
    private String supportingNames;    // tên các Supporting, cách nhau dấu phẩy
}