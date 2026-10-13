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
                            new SidebarItem("Duyệt bản thảo", "/department-head/dashboard", "clipboard-check", null))));
            // TODO: bổ sung menu khi làm màn hình cho các quyền còn lại
            case EDITORIAL_SECRETARY -> List.of(
                    new SidebarGroup("BIÊN TẬP TÒA SOẠN", List.of(
                            new SidebarItem("Duyệt cuối", "/secretary/final-reviews", "clipboard-check", null))));
            case REPORTER, ADMIN -> List.of();
        };
    }
}
