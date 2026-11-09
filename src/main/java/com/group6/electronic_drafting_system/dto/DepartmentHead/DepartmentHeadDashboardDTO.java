package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// DTO = "cái hộp đựng dữ liệu" của cả trang dashboard Trưởng ban. Class này KHÔNG làm gì, chỉ chứa dữ liệu.
//
// Hộp này đi qua 3 nơi:
//   1. TẠO + ĐIỀN: DepartmentHeadServiceImpl.getDashboard() dùng builder:
//        DepartmentHeadDashboardDTO.builder().displayName(...).stats(...).build()
//   2. CHUYỂN:     DepartmentHeadController.dashboard() bỏ vào model với tên "dashboard":
//        model.addAttribute("dashboard", hộp này)
//   3. ĐỌC:        templates/department_head/dashboard.html đọc bằng ${dashboard.<tên field>}
//        Ví dụ ${dashboard.stats} -> Thymeleaf gọi getStats() -> lấy field stats bên dưới.
//
// @Getter  (Lombok): tự sinh getDisplayName(), getStats()... để Thymeleaf đọc được. Không thấy trong file vì Lombok sinh lúc biên dịch.
// @Builder (Lombok): tự sinh builder để service điền từng field rồi gọi .build().
// Đổi tên một field ở đây thì phải sửa cả service (chỗ .tenField(...)) và dashboard.html (chỗ ${dashboard.tenField}).
@Getter
@Builder
public class DepartmentHeadDashboardDTO {
    // Họ tên Trưởng ban. Service điền: .displayName(head.getFullName()). HTML: ${dashboard.displayName} (dashboard.html dòng 58, 72)
    private String displayName;
    // Chữ viết tắt cho avatar, ví dụ "NH". Service điền: .userInitials(entryMapper.initials(...)). HTML: dashboard.html dòng 58
    private String userInitials;
    // Tên phòng ban; null khi Trưởng ban chưa được gán phòng ban. HTML: dashboard.html dòng 58, 61, 73
    private String departmentName;
    // 4 hộp thống kê đầu trang, mỗi phần tử là một DashboardStatDTO. HTML: th:each="stat : ${dashboard.stats}" (dashboard.html dòng 82-83)
    private List<DashboardStatDTO> stats;
    // Danh sách thẻ chờ duyệt (cả bài viết lẫn đề tài). HTML: th:each="entry : ${dashboard.entries}" (dashboard.html dòng 144)
    private List<DraftEntryDTO> entries;
    // Các chuyên mục (không trùng, A->Z) cho ô chọn "Chuyên mục". HTML: dashboard.html dòng 106
    private List<String> categories;
    // Số bài chờ duyệt -> số trên tab "Bài viết" và chip "Tất cả". HTML: dashboard.html dòng 94, 110
    private long articleCount;
    // Số đề tài chờ duyệt -> số trên tab "Đề tài". HTML: dashboard.html dòng 95
    private long topicCount;
    // Số bài hỏa tốc -> số trên chip "Hỏa tốc". HTML: dashboard.html dòng 111
    private long urgentCount;
}
