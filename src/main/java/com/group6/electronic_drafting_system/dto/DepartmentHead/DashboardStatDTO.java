package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Một hộp thống kê đầu trang dashboard (ví dụ "CHỜ DUYỆT BÀI VIẾT: 5 bản thảo"). Chỉ chứa dữ liệu, không làm gì.
//
// Đi qua 3 nơi:
//   1. TẠO:  DepartmentHeadServiceImpl.getDashboard() gọi new DashboardStatDTO(...) 4 lần (bước 6 trong service)
//   2. NẰM TRONG: field stats của DepartmentHeadDashboardDTO
//   3. ĐỌC:  dashboard.html dòng 83: th:each="stat : ${dashboard.stats}" -> mỗi vòng lặp, stat là một DashboardStatDTO
//            rồi ${stat.label}, ${stat.value}... gọi getLabel(), getValue()...
//
// @AllArgsConstructor (Lombok): tự sinh constructor nhận đủ 7 field THEO ĐÚNG THỨ TỰ khai báo bên dưới,
//   nên new DashboardStatDTO("CHỜ DUYỆT BÀI VIẾT", "5", "bản thảo", ...) phải truyền đúng thứ tự label, value, unit, note, badge, icon, tone.
// @Getter: tự sinh getLabel(), getValue()... cho Thymeleaf đọc.
@Getter
@AllArgsConstructor
public class DashboardStatDTO {
    private String label;  // 1. Tiêu đề hộp, ví dụ "CHỜ DUYỆT BÀI VIẾT"   -> ${stat.label}
    private String value;  // 2. Con số to, ví dụ "5"                       -> ${stat.value}
    private String unit;   // 3. Đơn vị cạnh số, ví dụ "bản thảo"           -> ${stat.unit}
    private String note;   // 4. Dòng ghi chú dưới cùng                     -> ${stat.note}
    private String badge;  // 5. Nhãn đỏ, ví dụ "2 HỎA TỐC"; null thì HTML ẩn nhãn (th:if) -> ${stat.badge}
    private String icon;   // 6. Tên biểu tượng, HTML ghép thành #dh-icon-<icon>, ví dụ #dh-icon-calendar
    private String tone;   // 7. Màu hộp: rose | blue | amber | slate. HTML đổi blue -> xanh lá, amber -> vàng, còn lại đỏ đô
}
