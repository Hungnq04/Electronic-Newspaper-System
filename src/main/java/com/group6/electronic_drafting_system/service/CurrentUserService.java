package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Header.CurrentUserDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.mapper.Reporter.ReporterTaskMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Thông tin người đang đăng nhập cho header chung: giống SidebarService, mọi role dùng chung một giao diện
@Service
public class CurrentUserService {
    private final AccountRepository accountRepository;

    public CurrentUserService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // readOnly: chỉ đọc. Mở transaction để lấy được role / phòng ban (hai quan hệ này tải LAZY)
    @Transactional(readOnly = true)
    public CurrentUserDTO findByEmail(String email) {
        if (email == null) return null;
        return accountRepository.findByEmailIgnoreCase(email).map(CurrentUserService::toDTO).orElse(null);
    }

    private static CurrentUserDTO toDTO(Account account) {
        String department = account.getDepartment() != null ? account.getDepartment().getDepartmentName() : null;
        return new CurrentUserDTO(account.getFullName(), roleLabel(account.getRole().getRoleName()), department,
                ReporterTaskMapper.initials(account.getFullName()));
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
