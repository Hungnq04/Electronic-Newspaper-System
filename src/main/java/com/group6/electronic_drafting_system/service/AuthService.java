package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Account.GetAccountDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    GetAccountDTO login(String email, String password);
    void persistSecurityContext(HttpServletRequest request);
    void sendRegistrationCode(String fullName, String email, String password);
    void resendRegistrationCode(String email);
    void verifyAndRegister(String email, String code);
    int getOtpRemainingSeconds(String email);
    void sendVerificationCodeForExistingAccount(String email);
    // Đăng ký bằng Google: email chưa có thì tạo tài khoản chờ Admin duyệt (AWAITING_APPROVAL); Google đã xác minh email
    void registerWithGoogle(String email, String fullName);
}
