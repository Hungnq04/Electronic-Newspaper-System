package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Thẻ thống kê đầu trang. icon: tên symbol trong sprite (#dh-icon-...), tone: rose | blue | amber | slate
@Getter
@AllArgsConstructor
public class DashboardStatDTO {
    private String label;
    private String value;
    private String unit;
    private String note;
    private String badge;
    private String icon;
    private String tone;
}
