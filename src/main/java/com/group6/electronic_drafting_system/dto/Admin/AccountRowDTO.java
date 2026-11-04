package com.group6.electronic_drafting_system.dto.Admin;

import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import java.time.OffsetDateTime;

// Một dòng trong màn Account List của Admin.
// canLock / canUnlock chỉ để hiện nút; service vẫn kiểm tra lại khi bấm.
public record AccountRowDTO(
        Integer id,
        String fullName,
        String username,
        String email,
        String roleLabel,
        String departmentName,
        AccountStatus status,
        String statusLabel,
        OffsetDateTime createdAt,
        boolean canLock,
        boolean canUnlock) {
}
