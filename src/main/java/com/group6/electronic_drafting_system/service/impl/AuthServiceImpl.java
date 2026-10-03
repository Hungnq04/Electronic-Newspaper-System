package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Account.GetAccountDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.EmailVerification;
import com.group6.electronic_drafting_system.entity.Role;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.Account.AccountMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.EmailVerificationRepository;
import com.group6.electronic_drafting_system.repository.RoleRepository;
import com.group6.electronic_drafting_system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final EmailVerificationRepository verifications;
    private final AccountMapper accountMapper;
    private final PasswordEncoder encoder;
    private final OtpEmailService otpEmailService;
    private final SecureRandom random = new SecureRandom();

    public AuthServiceImpl(AuthenticationManager authenticationManager, AccountRepository accounts,
                           RoleRepository roles, EmailVerificationRepository verifications,
                           AccountMapper accountMapper, PasswordEncoder encoder, OtpEmailService otpEmailService) {
        this.authenticationManager = authenticationManager;
        this.accounts = accounts;
        this.roles = roles;
        this.verifications = verifications;
        this.accountMapper = accountMapper;
        this.encoder = encoder;
        this.otpEmailService = otpEmailService;
    }

    @Override
    @Transactional
    public GetAccountDTO login(String email, String password) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        Account account = accounts.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        if (!Boolean.TRUE.equals(account.getVerified())) {
            SecurityContextHolder.clearContext();
            issueOtp(account);
            return accountMapper.toGetAccountDTO(account);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return accountMapper.toGetAccountDTO(account);
    }

    @Override
    public void persistSecurityContext(HttpServletRequest request) {
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
    }

    @Override
    @Transactional
    public void sendRegistrationCode(String fullName, String email, String password) {
        String normalizedEmail = normalize(email);
        if (accounts.existsByEmailIgnoreCase(normalizedEmail))
            throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_ALREADY_EXISTS);
        if (!normalizedEmail.endsWith("@gmail.com") || fullName == null || fullName.isBlank()
                || fullName.length() > 100 || password == null || password.length() < 8)
            throw new ApplicationException(ErrorCode.AUTH_REGISTRATION_FAILED);
        Role reporter = getReporterRole();
        Account account = new Account();
        account.setUsername(createUsername(normalizedEmail));
        account.setEmail(normalizedEmail);
        account.setFullName(fullName.trim());
        account.setPasswordHash(encoder.encode(password));
        account.setRole(reporter);
        account.setStatus("Active");
        account.setVerified(false);
        accounts.saveAndFlush(account);
        issueOtp(account);
    }

    @Override
    @Transactional
    public void resendRegistrationCode(String email) {
        String normalizedEmail = normalize(email);
        Account account = accounts.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_RESEND_OTP_FAILED));
        if (Boolean.TRUE.equals(account.getVerified())) {
            throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_ALREADY_EXISTS);
        }
        issueOtp(account);
    }

    @Override
    @Transactional
    public void verifyAndRegister(String email, String code) {
        String normalizedEmail = normalize(email);
        Account account = accounts.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        if (Boolean.TRUE.equals(account.getVerified())) throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_ALREADY_EXISTS);
        EmailVerification verification = verifications.findById(normalizedEmail)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_VERIFICATION_FAILED));
        if (verification.getExpiresAt().isBefore(LocalDateTime.now()) || code == null
                || !encoder.matches(code.trim(), verification.getCodeHash()))
            throw new ApplicationException(ErrorCode.AUTH_VERIFICATION_FAILED);
        account.setVerified(true);
        accounts.save(account);
        verifications.delete(verification);
    }

    @Override
    @Transactional(readOnly = true)
    public int getOtpRemainingSeconds(String email) {
        String normalizedEmail = normalize(email);
        return verifications.findById(normalizedEmail)
                .map(verification -> (int) Math.max(0,
                        java.time.Duration.between(LocalDateTime.now(), verification.getExpiresAt()).getSeconds()))
                .orElse(0);
    }

    @Override
    @Transactional
    public void sendVerificationCodeForExistingAccount(String email) {
        String normalizedEmail = normalize(email);
        Account account = accounts.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        if (!Boolean.TRUE.equals(account.getVerified())) issueOtp(account);
    }

    private String normalize(String email) {
        if (email == null || !email.matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$"))
            throw new ApplicationException(ErrorCode.AUTH_REGISTRATION_FAILED);
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void issueOtp(Account account) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        EmailVerification verification = verifications.findById(account.getEmail()).orElseGet(EmailVerification::new);
        verification.setEmail(account.getEmail());
        verification.setFullName(account.getFullName());
        verification.setPasswordHash(account.getPasswordHash());
        verification.setCodeHash(encoder.encode(code));
        verification.setExpiresAt(LocalDateTime.now().plusSeconds(60));
        verifications.saveAndFlush(verification);
        String recipient = verification.getEmail();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    otpEmailService.sendVerificationCode(recipient, code);
                }
            });
        } else {
            otpEmailService.sendVerificationCode(recipient, code);
        }
    }

    private Role getReporterRole() {
        return roles.findByRoleNameIgnoreCase("Reporter").orElseGet(() -> {
            Role role = new Role();
            role.setRoleName("Reporter");
            role.setDescription("Reporter account");
            return roles.save(role);
        });
    }

    private String createUsername(String normalizedEmail) {
        String username = normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
        if (username.length() > 50) username = username.substring(0, 50);
        if (accounts.existsByUsernameIgnoreCase(username)) {
            String suffix = "_" + Integer.toHexString(random.nextInt());
            username = username.substring(0, Math.min(username.length(), 50 - suffix.length())) + suffix;
        }
        return username;
    }
}
