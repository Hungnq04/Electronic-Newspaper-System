package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.Getter;
import lombok.Setter;

// Dữ liệu form "Thêm góp ý chung" (góp ý cho cả bài, không gắn vị trí)
//
// Đây là DTO theo chiều NGƯỢC: dữ liệu đi từ TRÌNH DUYỆT LÊN SERVER (khác ReviewCommentDTO là để hiển thị).
//
// ĐI QUA 3 NƠI:
//   1. HTML (workspacereview.html): form "Góp ý chung" (class ws-general-form) có <textarea name="content">,
//      gửi tới POST /department-head/articles/{id}/comments.
//   2. DepartmentHeadController.addComment(@ModelAttribute CommentRequestDTO commentRequest):
//        Spring tạo object này rồi đổ dữ liệu form vào THEO TÊN: name="content" -> setContent(...)
//        (@Setter của Lombok sinh sẵn hàm setXxx cho Spring dùng). Vì vậy tên field phải trùng name của ô nhập.
//   3. DepartmentHeadReviewServiceImpl.addComment() đọc bằng commentRequest.getContent() và kiểm tra không được rỗng.
@Getter
@Setter
public class CommentRequestDTO {
    private String content;           // nội dung góp ý (bắt buộc)
}
