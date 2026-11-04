package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Sidebar.SidebarGroup;
import com.group6.electronic_drafting_system.dto.Sidebar.SidebarItem;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import java.util.List;
import org.springframework.stereotype.Service;

// Menu sidebar theo từng quyền: cùng một giao diện (fragment/sidebar.html), chỉ khác dữ liệu
@Service
public class SidebarService {

    public List<SidebarGroup> menuFor(RoleName role) {
        return switch (role) {
            case DEPARTMENT_HEAD -> List.of(
                    new SidebarGroup("THẨM ĐỊNH NỘI DUNG", List.of(
                            new SidebarItem("Duyệt bản thảo", "/department-head/dashboard", "clipboard-check", null),
                            new SidebarItem("Duyệt đề tài", "/department-head/topics", "file-text", null))));
            case REPORTER -> List.of(
                    new SidebarGroup("TÁC NGHIỆP", List.of(
                            new SidebarItem("Danh sách đề tài", "/reporter/topics", "file-text", null),
                            new SidebarItem("Nhiệm vụ tác nghiệp", "/reporter/tasks", "clipboard-check", null),
                            new SidebarItem("Bài viết của tôi", "/articles", "edit", null))));
            // TODO: bổ sung menu khi làm màn hình cho các quyền còn lại
            case EDITORIAL_SECRETARY -> List.of(
                    new SidebarGroup("BIÊN TẬP TÒA SOẠN", List.of(
                            new SidebarItem("Topic tòa soạn", "/secretary/topics", "shapes", null),
                            new SidebarItem("Duyệt cuối", "/secretary/final-reviews", "clipboard-check", null),
                            // FL-01 Iter2: đổi URL sang /secretary/topic-reviews vì /secretary/topics là màn Topic tòa soạn (FL-02)
                            new SidebarItem("Duyệt đề tài cuối", "/secretary/topic-reviews", "file-text", null),
                            // FL-07 (Vanh): bài đã duyệt cuối, chờ xuất bản
                            new SidebarItem("Chờ xuất bản", "/secretary/publications", "file-text", null),
                            // FL-07 Iter3 (Vanh): bài đã hẹn giờ đăng (icon null = icon đồng hồ mặc định)
                            new SidebarItem("Đã hẹn giờ", "/secretary/scheduled-publications", null, null))));
            case ADMIN -> List.of(
                    new SidebarGroup("QUẢN TRỊ", List.of(
                            new SidebarItem("Tài khoản", "/admin/accounts", "users", null))));
        };
    }
}
