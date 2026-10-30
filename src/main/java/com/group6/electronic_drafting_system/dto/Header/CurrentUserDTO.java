package com.group6.electronic_drafting_system.dto.Header;

// Người đang đăng nhập, hiển thị trên header chung (fragment/header.html)
public record CurrentUserDTO(String fullName, String roleLabel, String departmentName, String initials) {

    // Dòng nhỏ dưới tên: "Phóng viên · Ban Thời sự", chưa có phòng ban thì chỉ hiện vai trò
    public String subtitle() {
        return departmentName == null ? roleLabel : roleLabel + " · " + departmentName;
    }
}
