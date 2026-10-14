package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Account.PendingAccountDTO;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import java.util.List;

// Admin duyệt tài khoản tự đăng ký (AWAITING_APPROVAL). Dành cho màn Admin; mọi quyền kiểm tra ở đây.
public interface AccountApprovalService {
    List<PendingAccountDTO> getAwaitingApproval(String adminEmail);

    // Cấp vai trò (chỉ REPORTER / DEPARTMENT_HEAD / EDITORIAL_SECRETARY) và phòng ban (bắt buộc với Trưởng ban) -> ACTIVE, gửi email
    void approve(Integer accountId, RoleName role, Integer departmentId, String adminEmail);

    // Từ chối -> INACTIVE (giữ dòng để truy vết), gửi email kèm lý do nếu có
    void reject(Integer accountId, String reason, String adminEmail);
}
