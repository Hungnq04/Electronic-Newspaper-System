package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.Getter;
import lombok.Setter;

// Dữ liệu form "Thêm góp ý". anchorField để trống = góp ý chung cho cả bài
//
// Đây là DTO theo chiều NGƯỢC: dữ liệu đi từ TRÌNH DUYỆT LÊN SERVER (khác ReviewCommentDTO là để hiển thị).
//
// ĐI QUA 3 NƠI:
//   1. HTML (workspacereview.html) có 2 form gửi tới POST /department-head/articles/{id}/comments:
//        - Form ẩn id="comment-form": 5 ô <input type="hidden" name="anchorField|anchorParagraph|anchorStart|anchorEnd|content">.
//          JS (popoverSave trong department-head-workspace-review.js) điền vị trí vừa bôi đen + nội dung rồi gửi.
//        - Form "Góp ý chung" (class ws-general-form): chỉ có <textarea name="content">, các ô vị trí để trống.
//   2. DepartmentHeadController.addComment(@ModelAttribute CommentRequestDTO commentRequest):
//        Spring tạo object này rồi đổ dữ liệu form vào THEO TÊN: name="content" -> setContent(...), ...
//        (@Setter của Lombok sinh sẵn các hàm setXxx cho Spring dùng). Vì vậy tên field phải trùng name của ô input.
//        Ở luồng tập code, bạn nhận 5 ô này bằng 5 @RequestParam riêng; ở đây gom vào 1 DTO cho gọn, kết quả như nhau.
//   3. DepartmentHeadReviewServiceImpl.addComment() đọc bằng commentRequest.getContent(), commentRequest.getAnchorField()...
//      và KIỂM TRA LẠI vị trí có hợp lệ không (không tin dữ liệu trình duyệt gửi lên).
@Getter
@Setter
public class CommentRequestDTO {
    private String anchorField;       // TITLE / SUBTITLE / SAPO / CONTENT; trống = góp ý chung
    private Integer anchorParagraph;  // đoạn thứ mấy (đếm từ 1)
    private Integer anchorStart;      // ký tự bắt đầu chỗ bôi đen (đếm từ 0)
    private Integer anchorEnd;        // ký tự kết thúc chỗ bôi đen (không tính)
    private String content;           // nội dung góp ý (bắt buộc)
}
