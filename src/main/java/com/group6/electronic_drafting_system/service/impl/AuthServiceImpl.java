package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Account.GetAccountDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.AccountOtp;
import com.group6.electronic_drafting_system.entity.Role;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.OtpPurpose;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.Account.AccountMapper;
import com.group6.electronic_drafting_system.repository.AccountOtpRepository;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.RoleRepository;
import com.group6.electronic_drafting_system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
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
    // Mã OTP có hiệu lực 60 giây kể từ last_sent_at, tối đa 5 lần nhập sai; gửi lại được bất cứ lúc nào.
    static final Duration OTP_VALIDITY = Duration.ofSeconds(60);
    // Chỉ để thỏa CHECK ck_otp_expiry của DB (expires_at = last_sent_at + 10 phút); hiệu lực thực tế là OTP_VALIDITY.
    static final Duration DB_EXPIRES_AT_OFFSET = Duration.ofMinutes(10);
    static final int OTP_MAX_ATTEMPTS = 5;

    private final AuthenticationManager authenticationManager;
    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final AccountOtpRepository otps;
    private final AccountMapper accountMapper;
    private final PasswordEncoder encoder;
    private final OtpEmailService otpEmailService;
    private final SecureRandom random = new SecureRandom();

    public AuthServiceImpl(AuthenticationManager authenticationManager, AccountRepository accounts,
                           RoleRepository roles, AccountOtpRepository otps,
                           AccountMapper accountMapper, PasswordEncoder encoder, OtpEmailService otpEmailService) {
        this.authenticationManager = authenticationManager;
        this.accounts = accounts;
        this.roles = roles;
        this.otps = otps;
        this.accountMapper = accountMapper;
        this.encoder = encoder;
        this.otpEmailService = otpEmailService;
    }

    @Override
    @Transactional
    public GetAccountDTO login(String email, String password) {
        if (email == null || password == null) throw new ApplicationException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        Account account = accounts.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        // Kiểm tra mật khẩu trước để không lộ trạng thái tài khoản cho người không biết mật khẩu
        if (!encoder.matches(password, account.getPasswordHash()))
            throw new ApplicationException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        switch (account.getStatus()) {
            case LOCKED -> throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_LOCKED);
            case INACTIVE -> throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_INACTIVE);
            // Mật khẩu đã đúng (kiểm tra ở trên) mới báo đang chờ duyệt
            case AWAITING_APPROVAL -> throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_AWAITING_APPROVAL);
            case PENDING -> {
                SecurityContextHolder.clearContext();
                issueOtpIfNoValidCode(account, OtpPurpose.VERIFY_EMAIL);
                return accountMapper.toGetAccountDTO(account);
            }
            case ACTIVE -> { }
        }
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(account.getEmail(), password));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return accountMapper.toGetAccountDTO(account);
    }

    @Override
    public void persistSecurityContext(HttpServletRequest request) {
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
    }

    @Override
    @Transactional(noRollbackFor = ApplicationException.class)
    public void sendRegistrationCode(String fullName, String email, String password) {
        String normalizedEmail = normalize(email);
        Optional<Account> existing = accounts.findByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            // Đã đăng ký nhưng chưa xác minh: gửi mã mới nếu mã hiện tại hết hiệu lực, rồi chuyển sang trang nhập OTP
            if (existing.get().getStatus() == AccountStatus.PENDING) {
                issueOtpIfNoValidCode(existing.get(), OtpPurpose.VERIFY_EMAIL);
                throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_VERIFIED);
            }
            throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_ALREADY_EXISTS);
        }
        if (!normalizedEmail.endsWith("@gmail.com") || fullName == null || fullName.isBlank()
                || fullName.trim().length() > 100 || password == null || password.length() < 8)
            throw new ApplicationException(ErrorCode.AUTH_REGISTRATION_FAILED);
        Account account = new Account();
        account.setUsername(createUsername(normalizedEmail));
        account.setEmail(normalizedEmail);
        account.setFullName(fullName.trim());
        account.setPasswordHash(encoder.encode(password));
        account.setRole(getReporterRole());
        account.setStatus(AccountStatus.PENDING);
        accounts.saveAndFlush(account);
        issueOtp(account, OtpPurpose.VERIFY_EMAIL);
    }

    @Override
    @Transactional
    public void resendRegistrationCode(String email) {
        Account account = accounts.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_RESEND_OTP_FAILED));
        if (account.getStatus() != AccountStatus.PENDING)
            throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_ALREADY_VERIFIED);
        issueOtp(account, OtpPurpose.VERIFY_EMAIL);
    }

    @Override
    @Transactional(noRollbackFor = ApplicationException.class)   // vẫn lưu attempt_count khi nhập sai
    public void verifyAndRegister(String email, String code) {
        Account account = accounts.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        if (account.getStatus() != AccountStatus.PENDING)
            throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_ALREADY_VERIFIED);
        AccountOtp otp = otps.findFirstByAccountAndPurposeAndUsedAtIsNullOrderByLastSentAtDescIdDesc(
                        account, OtpPurpose.VERIFY_EMAIL)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_VERIFICATION_FAILED));
        OffsetDateTime now = now();
        if (otp.getAttemptCount() >= OTP_MAX_ATTEMPTS)
            throw new ApplicationException(ErrorCode.AUTH_OTP_TOO_MANY_ATTEMPTS);
        if (now.isAfter(validUntil(otp)))
            throw new ApplicationException(ErrorCode.AUTH_OTP_EXPIRED);
        if (code == null || !code.trim().matches("\\d{6}") || !encoder.matches(code.trim(), otp.getCodeHash())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otps.save(otp);
            throw new ApplicationException(otp.getAttemptCount() >= OTP_MAX_ATTEMPTS
                    ? ErrorCode.AUTH_OTP_TOO_MANY_ATTEMPTS : ErrorCode.AUTH_VERIFICATION_FAILED);
        }
        otp.setUsedAt(now);
        otps.save(otp);
        // Web nội bộ: xác minh email xong vẫn phải chờ Admin duyệt và cấp vai trò (AccountApprovalService)
        account.setStatus(AccountStatus.AWAITING_APPROVAL);
        accounts.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public int getOtpRemainingSeconds(String email) {
        return findPendingOtp(email)
                .filter(otp -> otp.getAttemptCount() < OTP_MAX_ATTEMPTS)
                .map(otp -> secondsUntil(validUntil(otp)))
                .orElse(0);
    }

    @Override
    @Transactional
    public void sendVerificationCodeForExistingAccount(String email) {
        Account account = accounts.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        if (account.getStatus() == AccountStatus.PENDING) issueOtpIfNoValidCode(account, OtpPurpose.VERIFY_EMAIL);
    }

    @Override
    @Transactional
    public void registerWithGoogle(String email, String fullName) {
        String normalizedEmail = normalize(email);
        if (accounts.existsByEmailIgnoreCase(normalizedEmail)) return;
        // Cùng quy tắc với đăng ký bằng form: chỉ nhận Gmail
        if (!normalizedEmail.endsWith("@gmail.com")) throw new ApplicationException(ErrorCode.AUTH_REGISTRATION_FAILED);
        String name = fullName == null || fullName.isBlank()
                ? normalizedEmail.substring(0, normalizedEmail.indexOf('@')) : fullName.trim();
        Account account = new Account();
        account.setUsername(createUsername(normalizedEmail));
        account.setEmail(normalizedEmail);
        account.setFullName(name.length() > 100 ? name.substring(0, 100) : name);
        // Không có mật khẩu: lưu băm của chuỗi ngẫu nhiên (cột NOT NULL); muốn đăng nhập bằng mật khẩu thì dùng Quên mật khẩu
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        account.setPasswordHash(encoder.encode(java.util.HexFormat.of().formatHex(secret)));
        account.setRole(getReporterRole());
        // Google đã xác minh email nên bỏ qua OTP, nhưng vẫn chờ Admin duyệt và cấp vai trò như đăng ký form
        account.setStatus(AccountStatus.AWAITING_APPROVAL);
        accounts.saveAndFlush(account);
    }

    private String normalize(String email) {
        if (email == null || !email.trim().matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$"))
            throw new ApplicationException(ErrorCode.AUTH_REGISTRATION_FAILED);
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Optional<AccountOtp> findPendingOtp(String email) {
        if (email == null) return Optional.empty();
        return accounts.findByEmailIgnoreCase(email.trim())
                .filter(account -> account.getStatus() == AccountStatus.PENDING)
                .flatMap(account -> otps.findFirstByAccountAndPurposeAndUsedAtIsNullOrderByLastSentAtDescIdDesc(
                        account, OtpPurpose.VERIFY_EMAIL));
    }

    // Đăng nhập / đăng ký lại khi tài khoản còn PENDING: giữ mã đang còn hiệu lực, chỉ gửi mã mới khi cần
    private void issueOtpIfNoValidCode(Account account, OtpPurpose purpose) {
        boolean hasValidCode = otps.findFirstByAccountAndPurposeAndUsedAtIsNullOrderByLastSentAtDescIdDesc(account, purpose)
                .filter(otp -> otp.getAttemptCount() < OTP_MAX_ATTEMPTS && !now().isAfter(validUntil(otp)))
                .isPresent();
        if (!hasValidCode) issueOtp(account, purpose);
    }

    /**
     * Gửi mã OTP mới, kể cả khi mã cũ còn hiệu lực. Dùng lại dòng account_otp chưa dùng gần nhất
     * (ghi đè code_hash, last_sent_at, đặt lại attempt_count) nên mã cũ mất hiệu lực ngay lập tức,
     * mã mới có hiệu lực 60 giây tính từ bây giờ.
     */
    private void issueOtp(Account account, OtpPurpose purpose) {
        OffsetDateTime now = now();
        AccountOtp otp = otps.findFirstByAccountAndPurposeAndUsedAtIsNullOrderByLastSentAtDescIdDesc(account, purpose)
                .orElse(null);
        if (otp == null) {
            otp = new AccountOtp();
            otp.setAccount(account);
            otp.setPurpose(purpose);
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        otp.setCodeHash(encoder.encode(code));
        otp.setAttemptCount(0);
        otp.setLastSentAt(now);
        otp.setExpiresAt(now.plus(DB_EXPIRES_AT_OFFSET));
        otps.saveAndFlush(otp);
        String recipient = account.getEmail();
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

    // Cột thời gian là DATETIMEOFFSET(0): làm tròn xuống giây để khớp CHECK expires_at = last_sent_at + 10 phút
    private static OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static OffsetDateTime validUntil(AccountOtp otp) {
        return otp.getLastSentAt().plus(OTP_VALIDITY);
    }

    private static int secondsUntil(OffsetDateTime time) {
        return (int) Math.max(0, Duration.between(OffsetDateTime.now(), time).getSeconds());
    }

    private Role getReporterRole() {
        return roles.findByRoleName(RoleName.REPORTER).orElseGet(() -> {
            Role role = new Role();
            role.setRoleName(RoleName.REPORTER);
            role.setDescription("Phóng viên");
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
