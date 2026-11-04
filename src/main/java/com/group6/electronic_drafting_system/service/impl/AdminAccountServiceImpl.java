package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Admin.AccountDetailDTO;
import com.group6.electronic_drafting_system.dto.Admin.AccountDetailDTO.ActiveAssignmentDTO;
import com.group6.electronic_drafting_system.dto.Admin.AccountRowDTO;
import com.group6.electronic_drafting_system.dto.Admin.OptionDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.Role;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.DepartmentRepository;
import com.group6.electronic_drafting_system.repository.RoleRepository;
import com.group6.electronic_drafting_system.service.AdminAccountService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountServiceImpl implements AdminAccountService {
    // Admin chỉ cấp 3 vai trò này; không cấp ADMIN (tránh tự tạo thêm Admin)
    private static final List<RoleName> GRANTABLE_ROLES =
            List.of(RoleName.REPORTER, RoleName.DEPARTMENT_HEAD, RoleName.EDITORIAL_SECRETARY);

    private final AccountRepository accounts;
    private final AssignmentRepository assignments;
    private final RoleRepository roles;
    private final DepartmentRepository departments;

    public AdminAccountServiceImpl(AccountRepository accounts, AssignmentRepository assignments,
                                   RoleRepository roles, DepartmentRepository departments) {
        this.accounts = accounts;
        this.assignments = assignments;
        this.roles = roles;
        this.departments = departments;
    }

    // ===== Xem =====

    @Override
    @Transactional(readOnly = true)
    public List<AccountRowDTO> getAccounts(String adminEmail, AccountStatus status, RoleName role, String keyword) {
        requireAdmin(adminEmail);
        String key = keyword == null ? "" : keyword.trim().toLowerCase();

        List<AccountRowDTO> rows = new ArrayList<>();
        for (Account a : accounts.findAllByOrderByCreatedAtDescIdDesc()) {
            // Lọc đơn giản trong Java: số tài khoản của tòa soạn không lớn
            if (status != null && a.getStatus() != status) continue;
            if (role != null && a.getRole().getRoleName() != role) continue;
            if (!key.isEmpty() && !matches(a, key)) continue;

            boolean admin = a.getRole().getRoleName() == RoleName.ADMIN;
            rows.add(new AccountRowDTO(a.getId(), a.getFullName(), a.getUsername(), a.getEmail(),
                    roleLabel(a.getRole().getRoleName()), departmentName(a),
                    a.getStatus(), statusLabel(a.getStatus()), a.getCreatedAt(),
                    !admin && a.getStatus() == AccountStatus.ACTIVE,
                    !admin && a.getStatus() == AccountStatus.LOCKED));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus(String adminEmail) {
        requireAdmin(adminEmail);
        Map<String, Long> counts = new HashMap<>();
        for (AccountStatus s : AccountStatus.values()) counts.put(s.name(), 0L);
        for (Account a : accounts.findAll()) counts.merge(a.getStatus().name(), 1L, Long::sum);
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDetailDTO getDetail(Integer accountId, String adminEmail) {
        requireAdmin(adminEmail);
        Account a = findAccount(accountId);
        boolean admin = a.getRole().getRoleName() == RoleName.ADMIN;
        boolean hasHistory = accounts.hasEditorialHistory(a.getId()) == 1;

        List<ActiveAssignmentDTO> active = new ArrayList<>();
        for (Assignment s : activeAssignments(a.getId())) {
            active.add(new ActiveAssignmentDTO(s.getTopic().getId(), s.getTopic().getTitle(),
                    assignmentRoleLabel(s.getAssignmentRole()), s.getDeadline()));
        }

        boolean editableStatus = a.getStatus() == AccountStatus.ACTIVE || a.getStatus() == AccountStatus.LOCKED;
        return new AccountDetailDTO(a.getId(), a.getFullName(), a.getUsername(), a.getEmail(),
                a.getRole().getRoleName(), roleLabel(a.getRole().getRoleName()),
                a.getDepartment() == null ? null : a.getDepartment().getId(), departmentName(a),
                a.getStatus(), statusLabel(a.getStatus()), a.getCreatedAt(),
                active, hasHistory, admin,
                !admin && editableStatus && !hasHistory,
                !admin && a.getStatus() == AccountStatus.ACTIVE && active.isEmpty(),
                !admin && a.getStatus() == AccountStatus.LOCKED,
                a.getStatus() == AccountStatus.AWAITING_APPROVAL);
    }

    @Override
    public List<OptionDTO> getGrantableRoles() {
        List<OptionDTO> options = new ArrayList<>();
        for (RoleName r : GRANTABLE_ROLES) options.add(new OptionDTO(r.name(), roleLabel(r)));
        return options;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionDTO> getDepartments() {
        List<OptionDTO> options = new ArrayList<>();
        for (Department d : departments.findAll(Sort.by("departmentName"))) {
            options.add(new OptionDTO(String.valueOf(d.getId()), d.getDepartmentName()));
        }
        return options;
    }

    // ===== Đổi role / phòng ban =====

    @Override
    @Transactional
    public void changeRoleAndDepartment(Integer accountId, RoleName roleName, Integer departmentId, String adminEmail) {
        requireAdmin(adminEmail);
        Account a = findAccount(accountId);
        requireNotAdminAccount(a);

        // Chỉ đổi cho tài khoản đang dùng được (ACTIVE) hoặc đang bị khóa (LOCKED).
        // Tài khoản chờ duyệt thì dùng nút Duyệt; PENDING / INACTIVE thì không đổi.
        if (a.getStatus() != AccountStatus.ACTIVE && a.getStatus() != AccountStatus.LOCKED) {
            throw new ApplicationException(ErrorCode.ADMIN_ACCOUNT_STATUS_INVALID);
        }
        if (roleName == null || !GRANTABLE_ROLES.contains(roleName)) {
            throw new ApplicationException(ErrorCode.ACCOUNT_ROLE_NOT_ALLOWED);
        }
        // R20: Trưởng phòng bắt buộc có phòng ban
        if (roleName == RoleName.DEPARTMENT_HEAD && departmentId == null) {
            throw new ApplicationException(ErrorCode.ACCOUNT_DEPARTMENT_REQUIRED);
        }
        Department department = null;
        if (departmentId != null) {
            department = departments.findById(departmentId)
                    .orElseThrow(() -> new ApplicationException(ErrorCode.ACCOUNT_DEPARTMENT_REQUIRED));
        }

        Integer oldDepartmentId = a.getDepartment() == null ? null : a.getDepartment().getId();
        if (a.getRole().getRoleName() == roleName && Objects.equals(oldDepartmentId, departmentId)) {
            throw new ApplicationException(ErrorCode.ADMIN_NOTHING_CHANGED);
        }

        // Người đã tham gia biên tập thì không đổi (giữ đúng lịch sử, v_rule_violations phải = 0)
        if (accounts.hasEditorialHistory(a.getId()) == 1) {
            throw new ApplicationException(ErrorCode.ADMIN_CHANGE_HAS_HISTORY);
        }

        Role role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ACCOUNT_ROLE_NOT_ALLOWED));
        a.setRole(role);
        a.setDepartment(department);
        accounts.save(a);
    }

    // ===== Khóa / mở =====

    @Override
    @Transactional
    public void lock(Integer accountId, String adminEmail) {
        requireAdmin(adminEmail);
        Account a = findAccount(accountId);
        requireNotAdminAccount(a);
        if (a.getStatus() != AccountStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.ADMIN_ACCOUNT_STATUS_INVALID);
        }
        // R23: tài khoản không ACTIVE thì không được giữ assignment đang hiệu lực -> còn việc thì chặn
        if (!activeAssignments(a.getId()).isEmpty()) {
            throw new ApplicationException(ErrorCode.ADMIN_LOCK_HAS_ASSIGNMENT);
        }
        a.setStatus(AccountStatus.LOCKED);
        accounts.save(a);
    }

    @Override
    @Transactional
    public void unlock(Integer accountId, String adminEmail) {
        requireAdmin(adminEmail);
        Account a = findAccount(accountId);
        requireNotAdminAccount(a);
        if (a.getStatus() != AccountStatus.LOCKED) {
            throw new ApplicationException(ErrorCode.ADMIN_ACCOUNT_STATUS_INVALID);
        }
        a.setStatus(AccountStatus.ACTIVE);
        accounts.save(a);
    }

    // ===== Hàm phụ =====

    // Kiểm tra quyền ở backend: người gọi phải là Admin đang ACTIVE (BR-37, NFR-01)
    private void requireAdmin(String adminEmail) {
        boolean ok = adminEmail != null && accounts.findByEmailIgnoreCase(adminEmail.trim())
                .filter(a -> a.getRole().getRoleName() == RoleName.ADMIN && a.getStatus() == AccountStatus.ACTIVE)
                .isPresent();
        if (!ok) throw new ApplicationException(ErrorCode.ADMIN_FORBIDDEN);
    }

    // Không cho khóa / đổi vai trò tài khoản Admin (kể cả chính mình), tránh tự khóa mất quyền quản trị
    private void requireNotAdminAccount(Account a) {
        if (a.getRole().getRoleName() == RoleName.ADMIN) {
            throw new ApplicationException(ErrorCode.ADMIN_ACCOUNT_PROTECTED);
        }
    }

    private Account findAccount(Integer accountId) {
        if (accountId == null) throw new ApplicationException(ErrorCode.ACCOUNT_NOT_FOUND);
        return accounts.findDetailById(accountId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private List<Assignment> activeAssignments(Integer accountId) {
        return assignments.findByAccount_IdAndStatusOrderByAssignedAtDesc(accountId, AssignmentStatus.ASSIGNED);
    }

    private static boolean matches(Account a, String key) {
        return a.getFullName().toLowerCase().contains(key)
                || a.getUsername().toLowerCase().contains(key)
                || a.getEmail().toLowerCase().contains(key);
    }

    private static String departmentName(Account a) {
        return a.getDepartment() == null ? null : a.getDepartment().getDepartmentName();
    }

    private static String roleLabel(RoleName role) {
        return switch (role) {
            case REPORTER -> "Phóng viên";
            case DEPARTMENT_HEAD -> "Trưởng phòng";
            case EDITORIAL_SECRETARY -> "Thư ký tòa soạn";
            case ADMIN -> "Quản trị viên";
        };
    }

    private static String statusLabel(AccountStatus status) {
        return switch (status) {
            case PENDING -> "Chưa xác minh email";
            case AWAITING_APPROVAL -> "Chờ duyệt";
            case ACTIVE -> "Đang hoạt động";
            case LOCKED -> "Đã khóa";
            case INACTIVE -> "Ngừng hoạt động";
        };
    }

    private static String assignmentRoleLabel(AssignmentRole role) {
        return switch (role) {
            case DEPARTMENT_HEAD -> "Trưởng phòng phụ trách";
            case LEAD -> "Lead Reporter";
            case SUPPORTING -> "Supporting Reporter";
        };
    }
}
