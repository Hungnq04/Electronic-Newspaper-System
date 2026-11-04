package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Account.PendingAccountDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.Role;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.DepartmentRepository;
import com.group6.electronic_drafting_system.repository.RoleRepository;
import com.group6.electronic_drafting_system.service.AccountApprovalService;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountApprovalServiceImpl implements AccountApprovalService {
    // Admin không được cấp ADMIN qua luồng duyệt đăng ký (tránh tự nâng quyền)
    private static final Set<RoleName> GRANTABLE_ROLES =
            EnumSet.of(RoleName.REPORTER, RoleName.DEPARTMENT_HEAD, RoleName.EDITORIAL_SECRETARY);

    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final DepartmentRepository departments;
    private final AccountEmailService emailService;

    public AccountApprovalServiceImpl(AccountRepository accounts, RoleRepository roles,
                                      DepartmentRepository departments, AccountEmailService emailService) {
        this.accounts = accounts;
        this.roles = roles;
        this.departments = departments;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingAccountDTO> getAwaitingApproval(String adminEmail) {
        requireAdmin(adminEmail);
        return accounts.findByStatusOrderByCreatedAtAscIdAsc(AccountStatus.AWAITING_APPROVAL).stream()
                .map(a -> new PendingAccountDTO(a.getId(), a.getFullName(), a.getEmail(), a.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void approve(Integer accountId, RoleName roleName, Integer departmentId, String adminEmail) {
        requireAdmin(adminEmail);
        Account account = awaitingAccount(accountId);
        if (roleName == null || !GRANTABLE_ROLES.contains(roleName)) throw new ApplicationException(ErrorCode.ACCOUNT_ROLE_NOT_ALLOWED);
        // Phóng viên và Trưởng ban đều thuộc một phòng ban (Phóng viên cần phòng ban để đề xuất Topic cho đúng Trưởng ban)
        boolean needsDepartment = roleName == RoleName.DEPARTMENT_HEAD || roleName == RoleName.REPORTER;
        if (needsDepartment && departmentId == null) throw new ApplicationException(ErrorCode.ACCOUNT_DEPARTMENT_REQUIRED);
        Department department = departmentId == null ? null : departments.findById(departmentId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ACCOUNT_DEPARTMENT_REQUIRED));
        Role role = roles.findByRoleName(roleName).orElseThrow(() -> new ApplicationException(ErrorCode.ACCOUNT_ROLE_NOT_ALLOWED));

        account.setRole(role);
        account.setDepartment(department);
        account.setStatus(AccountStatus.ACTIVE);
        accounts.save(account);
        emailService.sendApproved(account.getEmail(), account.getFullName(), roleLabel(roleName),
                department == null ? null : department.getDepartmentName());
    }

    @Override
    @Transactional
    public void reject(Integer accountId, String reason, String adminEmail) {
        requireAdmin(adminEmail);
        Account account = awaitingAccount(accountId);
        // Quy ước nhóm: không xóa người dùng, chỉ đổi status
        account.setStatus(AccountStatus.INACTIVE);
        accounts.save(account);
        emailService.sendRejected(account.getEmail(), account.getFullName(), reason);
    }

    private void requireAdmin(String adminEmail) {
        boolean admin = adminEmail != null && accounts.findByEmailIgnoreCase(adminEmail.trim())
                .filter(a -> a.getRole().getRoleName() == RoleName.ADMIN && a.getStatus() == AccountStatus.ACTIVE)
                .isPresent();
        if (!admin) throw new ApplicationException(ErrorCode.ACCOUNT_APPROVAL_FORBIDDEN);
    }

    private Account awaitingAccount(Integer accountId) {
        Account account = accountId == null ? null : accounts.findById(accountId).orElse(null);
        if (account == null) throw new ApplicationException(ErrorCode.ACCOUNT_NOT_FOUND);
        if (account.getStatus() != AccountStatus.AWAITING_APPROVAL) throw new ApplicationException(ErrorCode.ACCOUNT_NOT_AWAITING_APPROVAL);
        return account;
    }

    private static String roleLabel(RoleName role) {
        return switch (role) {
            case REPORTER -> "Phóng viên";
            case DEPARTMENT_HEAD -> "Trưởng ban";
            case EDITORIAL_SECRETARY -> "Thư ký tòa soạn";
            case ADMIN -> "Quản trị viên";
        };
    }
}
