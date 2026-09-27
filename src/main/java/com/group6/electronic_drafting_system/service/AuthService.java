package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.EmailVerification;
import com.group6.electronic_drafting_system.entity.Role;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.EmailVerificationRepository;
import com.group6.electronic_drafting_system.repository.RoleRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final EmailVerificationRepository verifications;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AccountRepository accounts, RoleRepository roles, EmailVerificationRepository verifications,
                       PasswordEncoder encoder, JavaMailSender mailSender) {
        this.accounts = accounts; this.roles = roles; this.verifications = verifications;
        this.encoder = encoder; this.mailSender = mailSender;
    }

    @Transactional
    public void sendRegistrationCode(String fullName, String email, String password) {
        String normalizedEmail = normalize(email);
        if (!normalizedEmail.endsWith("@gmail.com")) throw new IllegalArgumentException("Đăng ký bằng OTP hiện chỉ hỗ trợ địa chỉ @gmail.com.");
        if (accounts.existsByEmailIgnoreCase(normalizedEmail)) throw new IllegalArgumentException("Email đã được đăng ký.");
        if (fullName == null || fullName.isBlank() || fullName.length() > 100) throw new IllegalArgumentException("Họ tên không hợp lệ.");
        if (password == null || password.length() < 8) throw new IllegalArgumentException("Mật khẩu cần có ít nhất 8 ký tự.");

        EmailVerification verification = verifications.findById(normalizedEmail).orElseGet(EmailVerification::new);
        verification.setEmail(normalizedEmail);
        verification.setFullName(fullName.trim());
        verification.setPasswordHash(encoder.encode(password));
        sendNewCode(verification);
    }

    @Transactional
    public void resendRegistrationCode(String email) {
        String normalizedEmail = normalize(email);
        EmailVerification verification = verifications.findById(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không có đăng ký đang chờ. Vui lòng đăng ký lại."));
        if (accounts.existsByEmailIgnoreCase(normalizedEmail)) {
            verifications.delete(verification);
            throw new IllegalArgumentException("Email đã được đăng ký.");
        }
        sendNewCode(verification);
    }

    @Transactional
    public void verifyAndRegister(String email, String code) {
        String normalizedEmail = normalize(email);
        EmailVerification verification = verifications.findById(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không có OTP đang chờ cho email này. Hãy gửi mã mới."));
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            verifications.delete(verification);
            throw new IllegalArgumentException("OTP đã hết hạn. Hãy gửi mã mới.");
        }
        if (code == null || !encoder.matches(code.trim(), verification.getCodeHash()))
            throw new IllegalArgumentException("OTP không đúng hoặc đã bị thay thế bởi mã mới nhất.");
        if (accounts.existsByEmailIgnoreCase(normalizedEmail)) {
            verifications.delete(verification);
            throw new IllegalArgumentException("Email đã được đăng ký.");
        }

        Role reporter = roles.findByRoleNameIgnoreCase("Reporter").orElseGet(() -> {
            Role role = new Role(); role.setRoleName("Reporter"); role.setDescription("Reporter account");
            return roles.save(role);
        });
        Account account = new Account();
        String username = normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
        if (username.length() > 50) username = username.substring(0, 50);
        if (accounts.existsByUsernameIgnoreCase(username)) {
            String suffix = "_" + Integer.toHexString(random.nextInt());
            username = username.substring(0, Math.min(username.length(), 50 - suffix.length())) + suffix;
        }
        account.setUsername(username);
        account.setEmail(normalizedEmail);
        account.setFullName(verification.getFullName());
        account.setPasswordHash(verification.getPasswordHash());
        account.setRole(reporter);
        account.setStatus("Active");
        accounts.save(account);
        verifications.delete(verification);
    }

    private String normalize(String email) {
        if (email == null || !email.matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$"))
            throw new IllegalArgumentException("Địa chỉ email không hợp lệ.");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void sendNewCode(EmailVerification verification) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        verification.setCodeHash(encoder.encode(code));
        verification.setExpiresAt(LocalDateTime.now().plusSeconds(60));
        verifications.saveAndFlush(verification); // The prior OTP is replaced before the new one is sent.
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(verification.getEmail());
        message.setSubject("Mã xác minh đăng ký Electronic Drafting System");
        message.setText("Mã OTP của bạn là " + code + ". Mã hết hạn sau 60 giây; chỉ mã được gửi gần nhất mới sử dụng được.");
        mailSender.send(message);
    }
}
