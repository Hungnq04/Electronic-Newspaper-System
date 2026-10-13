package com.group6.electronic_drafting_system.dto.Sidebar;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SidebarGroup {
    private String title;
    private List<SidebarItem> items;
}
