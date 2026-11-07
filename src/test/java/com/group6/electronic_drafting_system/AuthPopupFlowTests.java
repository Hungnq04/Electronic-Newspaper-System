package com.group6.electronic_drafting_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.OtpPurpose;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.entity.AccountOtp;
import com.group6.electronic_drafting_system.repository.AccountOtpRepository;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.RoleRepository;
import com.group6.electronic_drafting_system.service.AuthService;
import com.group6.electronic_drafting_system.service.impl.OtpEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// FL-01 Iter3: Register / OTP / Forgot / Reset. Email OTP được giả lập để lấy mã gốc.
// @Transactional: dữ liệu tạo trong test được rollback.
@SpringBootTest
@Transactional
class AuthPopupFlowTests {
    private static final String PASSWORD = "Matkhau123";

    @Autowired AuthService authService;
    @Autowired AccountRepository accounts;
    @Autowired AccountOtpRepository otps;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;
    @MockitoBean OtpEmailService otpEmailService;

    @Test
    void registrationValidatesEachField() {
        var errors = authService.validateRegistration("", "abc@yahoo.com", "abcdefgh", "khac");
        assertThat(errors).containsKeys("fullName", "email", "password", "confirmPassword");
        assertThat(errors.get("password")).isEqualTo("Mật khẩu phải có cả chữ và số.");
        assertThat(authService.validateRegistration("Nguyễn A", "fl01.iter3.ok@gmail.com", PASSWORD, PASSWORD)).isEmpty();
        // Email đã ACTIVE thì báo trùng
        assertThat(authService.validateRegistration("A", "chau.le@cmscore.example", PASSWORD, PASSWORD)).containsKey("email");
    }

    @Test
    void forgotPasswordThenResetChangesOnlyPassword() {
        Account account = newAccount("fl01.reset.test@gmail.com", AccountStatus.AWAITING_APPROVAL);
        authService.sendPasswordResetCode(account.getEmail());
        String code = sentCode(account.getEmail(), OtpPurpose.RESET_PASSWORD);

        // Gửi lại ngay: bị chặn 60 giây
        assertError(() -> authService.resendCode(account.getEmail(), OtpPurpose.RESET_PASSWORD), ErrorCode.AUTH_OTP_RESEND_TOO_SOON);
        assertThat(authService.getResendWaitSeconds(account.getEmail(), OtpPurpose.RESET_PASSWORD)).isGreaterThan(50);

        assertError(() -> authService.verifyResetCode(account.getEmail(), "000000".equals(code) ? "111111" : "000000"),
                ErrorCode.AUTH_VERIFICATION_FAILED);
        authService.verifyResetCode(account.getEmail(), code);
        // Mã chỉ dùng một lần
        assertError(() -> authService.verifyResetCode(account.getEmail(), code), ErrorCode.AUTH_VERIFICATION_FAILED);

        authService.resetPassword(account.getEmail(), "MatkhauMoi9", "MatkhauMoi9");
        Account saved = accounts.findByEmailIgnoreCase(account.getEmail()).orElseThrow();
        assertThat(encoder.matches("MatkhauMoi9", saved.getPasswordHash())).isTrue();
        assertThat(encoder.matches(PASSWORD, saved.getPasswordHash())).isFalse();
        // Không đổi status và role
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.AWAITING_APPROVAL);
        assertThat(saved.getRole().getRoleName()).isEqualTo(RoleName.REPORTER);
    }

    @Test
    void forgotPasswordIsSilentForUnknownOrLockedEmail() {
        Account locked = newAccount("fl01.locked.test@gmail.com", AccountStatus.LOCKED);
        // Không ném lỗi (controller vẫn hiện cùng thông báo) và không gửi mã
        authService.sendPasswordResetCode("khong.ton.tai.iter3@gmail.com");
        authService.sendPasswordResetCode(locked.getEmail());
        verify(otpEmailService, never()).sendVerificationCode(anyString(), anyString(), any());
        assertError(() -> authService.verifyResetCode(locked.getEmail(), "123456"), ErrorCode.AUTH_VERIFICATION_FAILED);
        assertError(() -> authService.resetPassword(locked.getEmail(), "MatkhauMoi9", "MatkhauMoi9"), ErrorCode.AUTH_RESET_NOT_ALLOWED);
    }

    @Test
    void wrongOtpFiveTimesDisablesCode() {
        Account account = newAccount("fl01.otp5.test@gmail.com", AccountStatus.ACTIVE);
        authService.sendPasswordResetCode(account.getEmail());
        String code = sentCode(account.getEmail(), OtpPurpose.RESET_PASSWORD);
        String wrong = "000000".equals(code) ? "111111" : "000000";
        for (int i = 0; i < 4; i++)
            assertError(() -> authService.verifyResetCode(account.getEmail(), wrong), ErrorCode.AUTH_VERIFICATION_FAILED);
        assertError(() -> authService.verifyResetCode(account.getEmail(), wrong), ErrorCode.AUTH_OTP_TOO_MANY_ATTEMPTS);
        // Mã đúng cũng không dùng được nữa
        assertError(() -> authService.verifyResetCode(account.getEmail(), code), ErrorCode.AUTH_OTP_TOO_MANY_ATTEMPTS);
    }

    @Test
    void googleConfirmsPendingAccountToAwaitingApproval() {
        Account account = newAccount("fl01.google.pending@gmail.com", AccountStatus.PENDING);
        authService.confirmEmailByGoogle(account.getEmail());
        assertThat(accounts.findByEmailIgnoreCase(account.getEmail()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.AWAITING_APPROVAL);
    }

    // Mail OTP chỉ gửi sau khi transaction commit; test luôn rollback nên không bắt được mã gốc.
    // Vì vậy ghi đè hash của dòng OTP vừa tạo bằng một mã đã biết.
    private String sentCode(String email, OtpPurpose purpose) {
        Account account = accounts.findByEmailIgnoreCase(email).orElseThrow();
        AccountOtp otp = otps.findFirstByAccountAndPurposeAndUsedAtIsNullOrderByLastSentAtDescIdDesc(account, purpose)
                .orElseThrow();
        otp.setCodeHash(encoder.encode("123456"));
        otps.saveAndFlush(otp);
        return "123456";
    }

    private Account newAccount(String email, AccountStatus status) {
        Account account = new Account();
        account.setUsername(email.substring(0, email.indexOf('@')));
        account.setEmail(email);
        account.setFullName("Người Thử Iter3");
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
