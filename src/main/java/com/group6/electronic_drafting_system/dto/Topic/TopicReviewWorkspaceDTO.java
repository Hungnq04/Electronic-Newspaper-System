package com.group6.electronic_drafting_system.dto.Topic;

import java.util.List;

// Màn duyệt đề tài (Trưởng ban / Thư ký): hàng chờ bên trái, chi tiết đề tài đang chọn bên phải.
// selected == null: chưa chọn đề tài nào (hàng chờ trống).
// canDecide: đề tài đang chọn còn chờ người này quyết định thì mới hiện nút Duyệt / Yêu cầu sửa / Từ chối.
// departments + departmentFilter: bộ lọc theo ban (chỉ dùng ở màn Thư ký).
// fullName + roleLabel + initials: người đăng nhập, hiện trên header (dùng chung fragment tpTopbar với màn Phóng viên).
public record TopicReviewWorkspaceDTO(String scopeLabel, List<TopicReviewItemDTO> queue, TopicReviewItemDTO selected,
                                      List<TopicHistoryEntryDTO> history, boolean canDecide,
                                      List<DepartmentOptionDTO> departments, Integer departmentFilter,
                                      String fullName, String roleLabel, String initials) {

    // Chuông thông báo chỉ dành cho phóng viên, nên ở màn duyệt luôn là 0 / rỗng (header vẫn dùng chung được)
    public long unreadNotifications() { return 0; }
    public List<NotificationItemDTO> notifications() { return List.of(); }
}
