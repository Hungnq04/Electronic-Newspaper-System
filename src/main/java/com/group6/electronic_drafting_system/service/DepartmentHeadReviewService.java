package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.DepartmentHead.CommentRequestDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewWorkspaceDTO;

// Trưởng phòng thẩm định một bài viết (FL-05): xem, góp ý, duyệt hoặc trả bài cho Lead
//
// Đây là INTERFACE = "menu": chỉ liệt kê có những việc gì, không có code bên trong.
// Code thật nằm ở service/impl/DepartmentHeadReviewServiceImpl.java (class có "implements DepartmentHeadReviewService").
// DepartmentHeadController khai báo biến kiểu interface này (departmentHeadReviewService); Spring đưa object Impl vào lúc khởi động.
//
// Mọi method đều nhận email người đăng nhập để service TỰ kiểm tra quyền lại từ đầu (không tin giao diện).
public interface DepartmentHeadReviewService {
    // Mở trang thẩm định: kiểm tra quyền, mở lượt duyệt nếu cần, gom mọi dữ liệu trang vào ReviewWorkspaceDTO.
    // Gọi từ: DepartmentHeadController.reviewWorkspace()  (GET /department-head/articles/{id}/review)
    ReviewWorkspaceDTO openWorkspace(String email, Integer articleId);

    // Thêm một góp ý (gắn vị trí trong bài, hoặc góp ý chung). Gọi từ: DepartmentHeadController.addComment()
    void addComment(String email, Integer articleId, CommentRequestDTO commentRequest);

    // Xóa góp ý của chính mình trong lượt duyệt đang mở. Gọi từ: DepartmentHeadController.deleteComment()
    void deleteComment(String email, Integer articleId, Integer commentId);

    // Đánh dấu góp ý VÒNG TRƯỚC là Lead đã sửa (RESOLVED) hay chưa (OPEN). Gọi từ: DepartmentHeadController.resolveComment()
    void setPreviousCommentResolved(String email, Integer articleId, Integer commentId, boolean resolved);

    // Duyệt cấp phòng: DEPARTMENT_REVIEW -> DEPARTMENT_APPROVED. Gọi từ: DepartmentHeadController.approve()
    void approve(String email, Integer articleId, String feedback);

    // Trả bài cho Lead: DEPARTMENT_REVIEW -> REVISION_REQUIRED, bắt buộc có nhận xét. Gọi từ: DepartmentHeadController.requestRevision()
    void requestRevision(String email, Integer articleId, String feedback);
}
