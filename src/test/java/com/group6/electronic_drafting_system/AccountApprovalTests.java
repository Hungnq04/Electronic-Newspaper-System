package com.group6.electronic_drafting_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.AccountOtp;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.OtpPurpose;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountOtpRepository;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.RoleRepository;
import com.group6.electronic_drafting_system.service.AccountApprovalService;
import com.group6.electronic_drafting_system.service.AuthService;
import com.group6.electronic_drafting_system.service.impl.AccountEmailService;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// Đăng ký -> xác minh email -> CHỜ ADMIN DUYỆT -> Admin cấp vai trò (ACTIVE) hoặc từ chối (INACTIVE).
// @Transactional: mọi tài khoản tạo trong test được rollback; email được giả lập, không gửi thật.
@SpringBootTest
@Transactional
class AccountApprovalTests {
    private static final String ADMIN = "admin@cmscore.example";
    private static final String PASSWORD = "Matkhau@123";

    @Autowired AccountApprovalService approvals;
    @Autowired AuthService authService;
    @Autowired AccountRepository accounts;
    @Autowired AccountOtpRepository otps;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;
    @MockitoBean AccountEmailService emailService;

    @Test
    void verifyingOtpLeavesAccountAwaitingApproval() {
        Account account = newAccount("fl01.otp.test@gmail.com", AccountStatus.PENDING);
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        AccountOtp otp = new AccountOtp();
        otp.setAccount(account);
        otp.setPurpose(OtpPurpose.VERIFY_EMAIL);
        otp.setCodeHash(encoder.encode("123456"));
        otp.setLastSentAt(now);
        otp.setExpiresAt(now.plusMinutes(10));
        otps.saveAndFlush(otp);

        authService.verifyAndRegister(account.getEmail(), "123456");

        assertThat(accounts.findByEmailIgnoreCase(account.getEmail()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.AWAITING_APPROVAL);
    }

    @Test
    void awaitingAccountCannotLogInButWrongPasswordStaysGeneric() {
        Account account = newAccount("fl01.awaiting.test@gmail.com", AccountStatus.AWAITING_APPROVAL);
        assertError(() -> authService.login(account.getEmail(), PASSWORD), ErrorCode.AUTH_ACCOUNT_AWAITING_APPROVAL);
        assertError(() -> authService.login(account.getEmail(), "sai-mat-khau"), ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @Test
    void adminApprovesWithRoleAndDepartmentThenAccountCanLogIn() {
        Account account = newAccount("fl01.approve.test@gmail.com", AccountStatus.AWAITING_APPROVAL);
        assertThat(approvals.getAwaitingApproval(ADMIN)).anyMatch(p -> p.email().equals(account.getEmail()));

        approvals.approve(account.getId(), RoleName.DEPARTMENT_HEAD, 4, ADMIN);

        Account approved = accounts.findByEmailIgnoreCase(account.getEmail()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(approved.getRole().getRoleName()).isEqualTo(RoleName.DEPARTMENT_HEAD);
        assertThat(approved.getDepartment().getId()).isEqualTo(4);
        verify(emailService).sendApproved(eq(account.getEmail()), anyString(), eq("Trưởng ban"), eq("Phòng Kinh tế"));
        assertThat(authService.login(account.getEmail(), PASSWORD).getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void adminRejectsToInactiveAndEmailsReason() {
        Account account = newAccount("fl01.reject.test@gmail.com", AccountStatus.AWAITING_APPROVAL);
        approvals.reject(account.getId(), "Không phải nhân sự tòa soạn", ADMIN);
        assertThat(accounts.findByEmailIgnoreCase(account.getEmail()).orElseThrow().getStatus()).isEqualTo(AccountStatus.INACTIVE);
        verify(emailService).sendRejected(account.getEmail(), account.getFullName(), "Không phải nhân sự tòa soạn");
    }

    @Test
    void approvalRules() {
        Account account = newAccount("fl01.rules.test@gmail.com", AccountStatus.AWAITING_APPROVAL);
        // Chỉ Admin mới duyệt được
        assertError(() -> approvals.approve(account.getId(), RoleName.REPORTER, 4, "chau.le@cmscore.example"), ErrorCode.ACCOUNT_APPROVAL_FORBIDDEN);
        // Không cấp ADMIN
        assertError(() -> approvals.approve(account.getId(), RoleName.ADMIN, null, ADMIN), ErrorCode.ACCOUNT_ROLE_NOT_ALLOWED);
        // Trưởng ban phải có phòng ban
        assertError(() -> approvals.approve(account.getId(), RoleName.DEPARTMENT_HEAD, null, ADMIN), ErrorCode.ACCOUNT_DEPARTMENT_REQUIRED);
        // Tài khoản đang hoạt động thì không đi qua luồng duyệt
        Integer activeId = accounts.findByEmailIgnoreCase("linh.ngo@cmscore.example").orElseThrow().getId();
        assertError(() -> approvals.approve(activeId, RoleName.EDITORIAL_SECRETARY, null, ADMIN), ErrorCode.ACCOUNT_NOT_AWAITING_APPROVAL);
        verify(emailService, never()).sendApproved(anyString(), anyString(), anyString(), any());
        assertThat(accounts.findByEmailIgnoreCase(account.getEmail()).orElseThrow().getStatus()).isEqualTo(AccountStatus.AWAITING_APPROVAL);
    }

    private Account newAccount(String email, AccountStatus status) {
        Account account = new Account();
        account.setUsername(email.substring(0, email.indexOf('@')));
        account.setEmail(email);
        account.setFullName("Người Đăng Ký Thử");
        account.setPasswordHash(encoder.encode(PASSWORD));
        account.setRole(roles.findByRoleName(RoleName.REPORTER).orElseThrow());
        account.setStatus(status);
        return accounts.saveAndFlush(account);
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOf(ApplicationException.class)
                .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode()).isEqualTo(expected));
    }
}
