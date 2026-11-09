package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// Dữ liệu của CẢ TRANG "Lịch sử duyệt" (UC-35).
//
// TẠO Ở ĐÂU: DepartmentHeadServiceImpl.getReviewHistory(email)
// GỬI SANG HTML: DepartmentHeadController.reviewHistory() -> model.addAttribute("history", ...)
// HTML ĐỌC:  ${history.displayName}, ${history.items}, ${history.approvedCount}...
@Getter
@Builder
public class ReviewHistoryPageDTO {
    private String displayName;              // tên Trưởng ban (thanh trên cùng)
    private String departmentName;           // tên phòng ban, có thể null
    private String userInitials;             // chữ viết tắt trên avatar, ví dụ "NH"
    private List<ReviewHistoryItemDTO> items; // các dòng của bảng, mới nhất trước
    private List<String> categories;         // các chuyên mục có trong bảng (không trùng, A -> Z) cho ô lọc
    private long totalCount;                 // tổng số lượt đã duyệt
    private long approvedCount;              // số lượt "Đã duyệt"
    private long revisionCount;              // số lượt "Yêu cầu sửa"
}
