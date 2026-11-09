package com.group6.electronic_drafting_system.dto.Admin;

import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import java.time.OffsetDateTime;
import java.util.List;

// Dữ liệu màn Account Detail của Admin
public record AccountDetailDTO(
        Integer id,
        String fullName,
        String username,
        String email,
        RoleName role,
        String roleLabel,
        Integer departmentId,
        String departmentName,
        AccountStatus status,
        String statusLabel,
        OffsetDateTime createdAt,
        List<ActiveAssignmentDTO> activeAssignments,  // việc đang làm (ASSIGNED)
        boolean hasEditorialHistory,                  // đã từng tham gia biên tập -> không đổi role / phòng ban
        boolean adminAccount,                         // tài khoản Admin: chỉ xem
        boolean canChangeRole,
        boolean canLock,
        boolean canUnlock,
        boolean awaitingApproval) {

    // Một việc đang làm: vai trò gì, trên Topic nào
    public record ActiveAssignmentDTO(Integer topicId, String topicTitle, String roleLabel, OffsetDateTime deadline) {
    }
}
