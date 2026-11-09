package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.Getter;
import lombok.Setter;

// Form "Đổi Lead" (EC-01): chọn Lead mới + nhiệm vụ và deadline của Lead mới.
// deadline dạng chuỗi từ <input type="datetime-local">, ví dụ "2026-10-30T17:00" (giờ Việt Nam).
@Getter @Setter
public class ChangeLeadFormDTO {
    private Integer newLeadAccountId;
    private String deadline;
    private String requirements;
}
