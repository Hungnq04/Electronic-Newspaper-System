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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
    // Mã OTP có hiệu lực 5 phút kể từ last_sent_at, tối đa 5 lần nhập sai.
    static final Duration OTP_VALIDITY = Duration.ofMinutes(5);
    // Phải chờ 60 giây kể từ lần gửi trước mới được gửi lại mã.
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    // Chỉ để thỏa CHECK ck_otp_expiry của DB (expires_at = last_sent_at + 10 phút); hiệu lực thực tế là OTP_VALIDITY.
    static final Duration DB_EXPIRES_AT_OFFSET = Duration.ofMinutes(10);
    static final int OTP_MAX_ATTEMPTS = 5;
    private static final String EMAIL_PATTERN = "(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$";

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
                // Chưa xác minh email: không cho đăng nhập, controller mở pop-up OTP
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

    // ===================== Đăng ký =====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> validateRegistration(String fullName, String email, String password, String confirmPassword) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (fullName == null || fullName.isBlank()) errors.put("fullName", "Vui lòng nhập họ tên.");
        else if (fullName.trim().length() > 100) errors.put("fullName", "Họ tên tối đa 100 ký tự.");

        String emailError = emailError(email);
        if (emailError != null) errors.put("email", emailError);
        else if (!email.trim().toLowerCase(Locale.ROOT).endsWith("@gmail.com"))
            errors.put("email", "Chỉ nhận địa chỉ Gmail (@gmail.com).");
        else {
            // Email đã có tài khoản (không phải PENDING) thì báo trùng; PENDING thì sendRegistrationCode mở lại pop-up OTP
            Optional<Account> existing = accounts.findByEmailIgnoreCase(email.trim());
            if (existing.isPresent() && existing.get().getStatus() != AccountStatus.PENDING)
                errors.put("email", ErrorCode.AUTH_ACCOUNT_ALREADY_EXISTS.getMessage());
        }

        errors.putAll(validateNewPassword(password, confirmPassword));
        return errors;
    }

    @Override
    @Transactional(noRollbackFor = ApplicationException.class)
    public void sendRegistrationCode(String fullName, String email, String password, String confirmPassword) {
        String normalizedEmail = normalize(email);
        Optional<Account> existing = accounts.findByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            // Đã đăng ký nhưng chưa xác minh: gửi mã mới nếu mã hiện tại hết hiệu lực, rồi mở pop-up OTP
            if (existing.get().getStatus() == AccountStatus.PENDING) {
                issueOtpIfNoValidCode(existing.get(), OtpPurpose.VERIFY_EMAIL);
                throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_VERIFIED);
            }
            throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_ALREADY_EXISTS);
        }
        // Kiểm tra lại ở backend (không tin vào form)
        if (!validateRegistration(fullName, email, password, confirmPassword).isEmpty())
            throw new ApplicationException(ErrorCode.AUTH_REGISTRATION_FAILED);
        Account account = new Account();
        account.setUsername(createUsername(normalizedEmail));
        account.setEmail(normalizedEmail);
        account.setFullName(fullName.trim());
        account.setPasswordHash(encoder.encode(password));
        // Role tạm là REPORTER (cột NOT NULL); Admin sẽ cấp role thật khi duyệt
        account.setRole(getReporterRole());
        account.setStatus(AccountStatus.PENDING);
        accounts.saveAndFlush(account);
        issueOtp(account, OtpPurpose.VERIFY_EMAIL);
    }

    @Override
    @Transactional(noRollbackFor = ApplicationException.class)   // vẫn lưu attempt_count khi nhập sai
    public void verifyAndRegister(String email, String code) {
        Account account = accounts.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        if (account.getStatus() != AccountStatus.PENDING)
            throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_ALREADY_VERIFIED);
        useOtp(account, OtpPurpose.VERIFY_EMAIL, code);
        // Web nội bộ: xác minh email xong vẫn phải chờ Admin duyệt và cấp vai trò (AccountApprovalService)
        account.setStatus(AccountStatus.AWAITING_APPROVAL);
        accounts.save(account);
    }

    // ===================== OTP dùng chung =====================

    @Override
    @Transactional
    public void resendCode(String email, OtpPurpose purpose) {
        Optional<Account> found = email == null ? Optional.empty() : accounts.findByEmailIgnoreCase(email.trim());
        if (purpose == OtpPurpose.RESET_PASSWORD) {
            // Quên mật khẩu: email không tồn tại / bị khóa thì im lặng, không cho người gọi biết
            if (found.isEmpty() || !canResetPassword(found.get())) return;
        } else {
            if (found.isEmpty()) throw new ApplicationException(ErrorCode.AUTH_RESEND_OTP_FAILED);
            if (found.get().getStatus() != AccountStatus.PENDING)
                throw new ApplicationException(ErrorCode.AUTH_ACCOUNT_ALREADY_VERIFIED);
        }
        Account account = found.get();
        // Chưa đủ 60 giây kể từ lần gửi trước thì không gửi lại
        if (resendWaitSeconds(account, purpose) > 0)
            throw new ApplicationException(ErrorCode.AUTH_OTP_RESEND_TOO_SOON);
        issueOtp(account, purpose);
    }

    @Override
    @Transactional(readOnly = true)
    public int getResendWaitSeconds(String email, OtpPurpose purpose) {
        if (email == null) return 0;
        return accounts.findByEmailIgnoreCase(email.trim())
                .map(account -> resendWaitSeconds(account, purpose))
                .orElse(0);
    }

    // ===================== Quên / đặt lại mật khẩu =====================

    @Override
    @Transactional
    public void sendPasswordResetCode(String email) {
        String error = emailError(email);
        if (error != null) throw new ApplicationException(ErrorCode.AUTH_EMAIL_INVALID);
        Optional<Account> found = accounts.findByEmailIgnoreCase(email.trim());
        // Chỉ gửi mã thật cho ACTIVE / AWAITING_APPROVAL. Các trường hợp khác im lặng (controller vẫn hiện cùng thông báo)
        if (found.isEmpty() || !canResetPassword(found.get())) return;
        // Bấm "Gửi mã" liên tục trong 60 giây: giữ mã cũ, không gửi thêm
        if (resendWaitSeconds(found.get(), OtpPurpose.RESET_PASSWORD) > 0) return;
        issueOtp(found.get(), OtpPurpose.RESET_PASSWORD);
    }

    @Override
    @Transactional(noRollbackFor = ApplicationException.class)   // vẫn lưu attempt_count khi nhập sai
    public void verifyResetCode(String email, String code) {
        Account account = (email == null ? Optional.<Account>empty() : accounts.findByEmailIgnoreCase(email.trim()))
                .filter(this::canResetPassword)
                // Email không tồn tại / bị khóa: báo lỗi chung như nhập sai mã
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_VERIFICATION_FAILED));
        useOtp(account, OtpPurpose.RESET_PASSWORD, code);
    }

    @Override
    public Map<String, String> validateNewPassword(String password, String confirmPassword) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (password == null || password.length() < 8)
            errors.put("password", "Mật khẩu phải có ít nhất 8 ký tự.");
        else if (!password.matches(".*\\p{L}.*") || !password.matches(".*\\d.*"))
            errors.put("password", "Mật khẩu phải có cả chữ và số.");
        if (confirmPassword == null || !confirmPassword.equals(password))
            errors.put("confirmPassword", "Mật khẩu nhập lại không khớp.");
        return errors;
    }

    @Override
    @Transactional
    public void resetPassword(String email, String password, String confirmPassword) {
        if (!validateNewPassword(password, confirmPassword).isEmpty())
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED);
        Account account = (email == null ? Optional.<Account>empty() : accounts.findByEmailIgnoreCase(email.trim()))
                .filter(this::canResetPassword)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_RESET_NOT_ALLOWED));
        // Chỉ đổi mật khẩu (BCrypt), KHÔNG đổi status và role
        account.setPasswordHash(encoder.encode(password));
        accounts.save(account);
    }

    // ===================== Google =====================

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

    @Override
    @Transactional
    public void confirmEmailByGoogle(String email) {
        Account account = accounts.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        // Google đã xác minh email thay cho OTP: PENDING -> AWAITING_APPROVAL (vẫn chờ Admin, không vào hệ thống)
        if (account.getStatus() == AccountStatus.PENDING) {
            account.setStatus(AccountStatus.AWAITING_APPROVAL);
            accounts.save(account);
        }
    }

    // ===================== Hàm phụ =====================

    private String normalize(String email) {
        if (emailError(email) != null) throw new ApplicationException(ErrorCode.AUTH_REGISTRATION_FAILED);
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // Trả về câu lỗi nếu email trống / sai định dạng; null nếu hợp lệ
    private static String emailError(String email) {
        if (email == null || email.isBlank()) return "Vui lòng nhập email.";
        if (!email.trim().matches(EMAIL_PATTERN)) return ErrorCode.AUTH_EMAIL_INVALID.getMessage();
        return null;
    }

    // Chỉ tài khoản ACTIVE hoặc AWAITING_APPROVAL được đặt lại mật khẩu
    private boolean canResetPassword(Account account) {
        return account.getStatus() == AccountStatus.ACTIVE || account.getStatus() == AccountStatus.AWAITING_APPROVAL;
    }

    /**
     * Kiểm tra mã OTP (dùng chung cho đăng ký và quên mật khẩu):
     * sai quá 5 lần thì vô hiệu; quá 5 phút thì hết hạn; sai thì tăng attempt_count; đúng thì đánh dấu đã dùng.
     */
    private void useOtp(Account account, OtpPurpose purpose, String code) {
        AccountOtp otp = otps.findFirstByAccountAndPurposeAndUsedAtIsNullOrderByLastSentAtDescIdDesc(account, purpose)
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
        // Mã chỉ dùng được một lần
        otp.setUsedAt(now);
        otps.save(otp);
    }

    // Số giây còn phải chờ trước khi được gửi lại mã (tính từ last_sent_at của mã chưa dùng gần nhất)
    private int resendWaitSeconds(Account account, OtpPurpose purpose) {
        return otps.findFirstByAccountAndPurposeAndUsedAtIsNullOrderByLastSentAtDescIdDesc(account, purpose)
                .map(otp -> secondsUntil(otp.getLastSentAt().plus(RESEND_COOLDOWN)))
                .orElse(0);
    }

    // Đăng nhập / đăng ký lại khi tài khoản còn PENDING: giữ mã đang còn hiệu lực, chỉ gửi mã mới khi cần
    private void issueOtpIfNoValidCode(Account account, OtpPurpose purpose) {
        boolean hasValidCode = otps.findFirstByAccountAndPurposeAndUsedAtIsNullOrderByLastSentAtDescIdDesc(account, purpose)
                .filter(otp -> otp.getAttemptCount() < OTP_MAX_ATTEMPTS && !now().isAfter(validUntil(otp)))
                .isPresent();
        if (!hasValidCode) issueOtp(account, purpose);
    }

    /**
     * Gửi mã OTP mới. Dùng lại dòng account_otp chưa dùng gần nhất
     * (ghi đè code_hash, last_sent_at, đặt lại attempt_count) nên mã cũ mất hiệu lực ngay lập tức,
     * mã mới có hiệu lực 5 phút tính từ bây giờ.
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
        // Chỉ lưu hash của mã, không lưu mã gốc
        otp.setCodeHash(encoder.encode(code));
        otp.setAttemptCount(0);
        otp.setLastSentAt(now);
        otp.setExpiresAt(now.plus(DB_EXPIRES_AT_OFFSET));
        otps.saveAndFlush(otp);
        String recipient = account.getEmail();
        // Gửi mail sau khi transaction commit thành công (nếu lỗi rollback thì không gửi mã "ma")
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    otpEmailService.sendVerificationCode(recipient, code, purpose);
                }
            });
        } else {
            otpEmailService.sendVerificationCode(recipient, code, purpose);
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
