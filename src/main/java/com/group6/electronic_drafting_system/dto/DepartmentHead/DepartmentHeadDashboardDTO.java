package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// DTO = "cái hộp đựng dữ liệu" của cả trang dashboard Trưởng ban. Class này KHÔNG làm gì, chỉ chứa dữ liệu.
//
// Hộp này đi qua 3 nơi:
//   1. TẠO + ĐIỀN: DepartmentHeadServiceImpl.getDashboard() dùng builder:
//        DepartmentHeadDashboardDTO.builder().displayName(...).entries(...).build()
//   2. CHUYỂN:     DepartmentHeadController.dashboard() bỏ vào model với tên "dashboard":
//        model.addAttribute("dashboard", hộp này)
//   3. ĐỌC:        templates/department_head/dashboard.html đọc bằng ${dashboard.<tên field>}
//        Ví dụ ${dashboard.entries} -> Thymeleaf gọi getEntries() -> lấy field entries bên dưới.
//
// @Getter  (Lombok): tự sinh getDisplayName(), getEntries()... để Thymeleaf đọc được. Không thấy trong file vì Lombok sinh lúc biên dịch.
// @Builder (Lombok): tự sinh builder để service điền từng field rồi gọi .build().
// Đổi tên một field ở đây thì phải sửa cả service (chỗ .tenField(...)) và dashboard.html (chỗ ${dashboard.tenField}).
@Getter
@Builder
public class DepartmentHeadDashboardDTO {
    // Họ tên Trưởng ban. Service điền: .displayName(head.getFullName()). HTML: ${dashboard.displayName} (dashboard.html dòng 53, 67)
    private String displayName;
    // Chữ viết tắt cho avatar, ví dụ "NH". Service điền: .userInitials(entryMapper.initials(...)). HTML: dashboard.html dòng 53
    private String userInitials;
    // Tên phòng ban; null khi Trưởng ban chưa được gán phòng ban. HTML: dashboard.html dòng 53, 56, 68
    private String departmentName;
    // Danh sách bài viết chờ duyệt (mỗi dòng bảng). HTML: th:each="card : ${dashboard.entries}" (dashboard.html dòng 97)
    private List<DraftEntryDTO> entries;
}
