package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.Builder;
import lombok.Getter;

// MỘT DÒNG trong bảng "Lịch sử duyệt" = một lượt duyệt cấp phòng mà Trưởng ban đã chốt (Duyệt hoặc Yêu cầu sửa).
//
// TẠO Ở ĐÂU: DepartmentHeadServiceImpl.getReviewHistory() -> hàm phụ toHistoryItem(...)
// NẰM TRONG: ReviewHistoryPageDTO.items
// HTML ĐỌC:  th:each="item : ${history.items}" trong templates/department_head/review-history.html
//
// DTO (Data Transfer Object) = "hộp" chỉ chứa dữ liệu để gửi sang HTML, không có logic.
// @Getter: Lombok tự tạo getArticleTitle()... để HTML đọc được ${item.articleTitle}
// @Builder: tạo object bằng ReviewHistoryItemDTO.builder().articleId(...).build()
@Getter
@Builder
public class ReviewHistoryItemDTO {
    private Integer articleId;          // mã bài -> ghép link "Xem bài" (/department-head/articles/{id}/review)
    private String articleCode;         // mã hiển thị, ví dụ "BT-15"
    private String articleTitle;        // tít của PHIÊN BẢN đã duyệt (đúng bản mình đã đọc lúc đó)
    private String topicTitle;          // tên đề tài
    private String category;            // chuyên mục -> dùng cho ô lọc "Chuyên mục"
    private String versionLabel;        // phiên bản đã duyệt, ví dụ "1.0", "1.1"
    private boolean approved;           // true = Đã duyệt, false = Yêu cầu sửa -> dùng cho ô lọc "Quyết định"
    private String decisionText;        // chữ hiển thị: "Đã duyệt" / "Yêu cầu sửa"
    private String feedback;            // nhận xét lúc chốt (Duyệt thì có thể để trống)
    private long commentCount;          // số góp ý mình đã viết trong lượt duyệt đó
    private String reviewedAtText;      // thời điểm chốt, đã định dạng sẵn "HH:mm dd/MM/yyyy"
    private String articleStatusText;   // trạng thái HIỆN TẠI của bài, ví dụ "Đã xuất bản"
    private String articleStatusGroup;  // nhóm màu của nhãn trạng thái: doing / review / done (class rp-stage--...)
}
