package com.group6.electronic_drafting_system.dto.Sidebar;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Một mục menu trong fragment/sidebar.html (icon: edit, clipboard-check, folder-image, shapes, file-text, users, settings)
@Getter
@AllArgsConstructor
public class SidebarItem {
    private String label;
    private String href;
    private String icon;
    private Integer count;
}
