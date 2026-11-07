package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Admin.AccountDetailDTO;
import com.group6.electronic_drafting_system.dto.Admin.AccountRowDTO;
import com.group6.electronic_drafting_system.dto.Admin.OptionDTO;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import java.util.List;
import java.util.Map;

// Admin quản lý tài khoản (FR-15, BR-37): xem danh sách, đổi role / phòng ban, khóa / mở.
// Duyệt tài khoản mới đăng ký nằm ở AccountApprovalService (TV1).
// Mọi hàm đều kiểm tra người gọi là Admin đang ACTIVE.
public interface AdminAccountService {

    // status / role / keyword có thể null = không lọc
    List<AccountRowDTO> getAccounts(String adminEmail, AccountStatus status, RoleName role, String keyword);

    // Số tài khoản theo từng trạng thái (cho các tab lọc), key là tên trạng thái, vd "ACTIVE"
    Map<String, Long> countByStatus(String adminEmail);

    AccountDetailDTO getDetail(Integer accountId, String adminEmail);

    // Vai trò Admin được cấp: REPORTER / DEPARTMENT_HEAD / EDITORIAL_SECRETARY
    List<OptionDTO> getGrantableRoles();

    List<OptionDTO> getDepartments();

    void changeRoleAndDepartment(Integer accountId, RoleName role, Integer departmentId, String adminEmail);

    void lock(Integer accountId, String adminEmail);

    void unlock(Integer accountId, String adminEmail);
}
