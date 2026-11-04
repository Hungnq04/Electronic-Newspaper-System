package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.service.AuthService;
import com.group6.electronic_drafting_system.dto.Account.LoginRequestDTO;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.OtpPurpose;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * FL-01 Iter3: Login, Register, OTP, Forgot / Reset Password đều là pop-up trên trang /login.
 * Controller chỉ báo cho template biết mở pop-up nào (biến "popup"):
 * login, register, otp, awaiting, forgot, reset.
 * Email đang chờ nhập OTP và email được phép đặt lại mật khẩu được giữ trong session (không tin vào form / URL).
 */
@Controller
public class AuthController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    // Khóa lưu trong session
    static final String OTP_EMAIL = "OTP_EMAIL";          // email đang chờ nhập OTP
    static final String OTP_PURPOSE = "OTP_PURPOSE";      // VERIFY_EMAIL hoặc RESET_PASSWORD
    static final String OTP_SENT_AT = "OTP_SENT_AT";      // thời điểm bấm gửi mã (ms), để đếm ngược 60 giây trên giao diện
    static final String RESET_EMAIL = "RESET_EMAIL";      // chỉ có sau khi nhập đúng OTP quên mật khẩu
    private static final int RESEND_SECONDS = 60;

    static final String FORGOT_SENT_MESSAGE = "Nếu email đã đăng ký, mã xác nhận đã được gửi.";

    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    // ===================== Trang /login (chứa tất cả pop-up) =====================

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String awaiting, @RequestParam(required = false) String registered,
                            @RequestParam(name = "popup", required = false) String popupParam,
                            HttpSession session, Model model) {
        // Lỗi từ Spring Security / Google gửi qua URL (?error=CODE)
        if (error != null) model.addAttribute("error", ErrorCode.fromName(error).getMessage());
        if (logout != null) model.addAttribute("message", "Bạn đã đăng xuất.");
        // Tài khoản chờ Admin duyệt: là thông báo (khung xanh), không phải lỗi
        if (awaiting != null) model.addAttribute("message", ErrorCode.AUTH_ACCOUNT_AWAITING_APPROVAL.getMessage());
        if (registered != null) model.addAttribute("message",
                "Đăng ký bằng Google thành công. " + ErrorCode.AUTH_ACCOUNT_AWAITING_APPROVAL.getMessage());

        // Pop-up cần mở: ưu tiên giá trị flash (sau khi submit form), sau đó tới ?popup= trên URL
        String popup = (String) model.getAttribute("popup");
        if (popup == null) popup = popupParam;
        popup = allowedPopup(popup, session);
        model.addAttribute("popup", popup);

        if (!model.containsAttribute("fieldErrors")) model.addAttribute("fieldErrors", Map.of());
        if ("otp".equals(popup)) {
            model.addAttribute("otpEmail", session.getAttribute(OTP_EMAIL));
            model.addAttribute("otpPurpose", session.getAttribute(OTP_PURPOSE));
            model.addAttribute("resendWait", resendWaitSeconds(session));
        }
        return "login";
    }

    // Không cho mở pop-up OTP / Reset bằng cách gõ URL khi chưa tới bước đó
    private String allowedPopup(String popup, HttpSession session) {
        if (popup == null) return "login";
        return switch (popup) {
            case "register", "forgot", "awaiting", "login" -> popup;
            case "otp" -> session.getAttribute(OTP_EMAIL) != null ? "otp" : "login";
            case "reset" -> session.getAttribute(RESET_EMAIL) != null ? "reset" : "login";
            default -> "login";
        };
    }

    // Route cũ: chuyển về /login và mở pop-up tương ứng
    @GetMapping("/register")
    public String registerPage() { return "redirect:/login?popup=register"; }

    @GetMapping("/register/verify")
    public String verifyPage() { return "redirect:/login?popup=otp"; }

    @GetMapping({"/forgot-password", "/reset-password"})
    public String forgotPage(HttpServletRequest request) {
        return request.getRequestURI().endsWith("/reset-password") ? "redirect:/login?popup=reset" : "redirect:/login?popup=forgot";
    }

    // ===================== Đăng nhập =====================

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequestDTO loginRequest, HttpServletRequest request,
                        RedirectAttributes redirect) {
        try {
            var account = authService.login(loginRequest.getEmail(), loginRequest.getPassword());
            if (account.getStatus() == AccountStatus.PENDING) {
                // Chưa xác minh email: mở pop-up OTP (mã đã được gửi nếu mã cũ hết hạn)
                startOtp(request.getSession(true), account.getEmail(), OtpPurpose.VERIFY_EMAIL, false);
                redirect.addFlashAttribute("popup", "otp");
                redirect.addFlashAttribute("message", ErrorCode.AUTH_ACCOUNT_NOT_VERIFIED.getMessage()
                        + " Vui lòng nhập mã xác thực đã gửi đến email của bạn.");
                return "redirect:/login";
            }
            authService.persistSecurityContext(request); // Lưu thông tin đăng nhập vào SecurityContext để Spring Security nhận diện người dùng
            return "redirect:/dashboard";
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("email", loginRequest.getEmail());
            if (ex.getErrorCode() == ErrorCode.AUTH_ACCOUNT_AWAITING_APPROVAL) {
                redirect.addFlashAttribute("message", ex.getErrorCode().getMessage());
            } else {
                redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            }
            return "redirect:/login";
        } catch (AuthenticationException ex) {
            redirect.addFlashAttribute("email", loginRequest.getEmail());
            redirect.addFlashAttribute("error", ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage());
            return "redirect:/login";
        }
    }

    // ===================== Đăng ký =====================

    @PostMapping("/register")
    public String register(@RequestParam(required = false) String fullName, @RequestParam(required = false) String email,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) String confirmPassword,
                           HttpSession session, RedirectAttributes redirect) {
        // Kiểm tra từng ô; có lỗi thì mở lại pop-up Đăng ký, giữ họ tên + email (không giữ mật khẩu)
        Map<String, String> errors = authService.validateRegistration(fullName, email, password, confirmPassword);
        if (!errors.isEmpty()) return backToRegister(redirect, fullName, email, errors, null);
        try {
            authService.sendRegistrationCode(fullName, email, password, confirmPassword);
            startOtp(session, email.trim().toLowerCase(), OtpPurpose.VERIFY_EMAIL, true);
            redirect.addFlashAttribute("message", "Mã OTP đã được gửi đến email của bạn. Mã có hiệu lực trong 5 phút.");
        } catch (ApplicationException ex) {
            if (ex.getErrorCode() == ErrorCode.AUTH_ACCOUNT_NOT_VERIFIED) {
                // Email đã đăng ký nhưng chưa xác minh: mở pop-up OTP
                startOtp(session, email.trim().toLowerCase(), OtpPurpose.VERIFY_EMAIL, false);
                redirect.addFlashAttribute("message", ErrorCode.AUTH_ACCOUNT_NOT_VERIFIED.getMessage()
                        + " Vui lòng nhập mã xác thực đã gửi đến email của bạn.");
            } else {
                return backToRegister(redirect, fullName, email, Map.of(), ex.getErrorCode().getMessage());
            }
        } catch (Exception ex) {
            log.error("Register failed for {}", email, ex);
            return backToRegister(redirect, fullName, email, Map.of(), ErrorCode.AUTH_UNEXPECTED_ERROR.getMessage());
        }
        redirect.addFlashAttribute("popup", "otp");
        return "redirect:/login";
    }

    private String backToRegister(RedirectAttributes redirect, String fullName, String email,
                                  Map<String, String> errors, String error) {
        redirect.addFlashAttribute("popup", "register");
        redirect.addFlashAttribute("fieldErrors", errors);
        redirect.addFlashAttribute("fullName", fullName);
        redirect.addFlashAttribute("email", email);
        if (error != null) redirect.addFlashAttribute("error", error);
        return "redirect:/login";
    }

    // ===================== OTP (dùng chung đăng ký + quên mật khẩu) =====================

    @PostMapping("/otp/verify")
    public String verifyOtp(@RequestParam(name = "digit", required = false) List<String> digits,
                            HttpSession session, RedirectAttributes redirect) {
        String email = (String) session.getAttribute(OTP_EMAIL);
        OtpPurpose purpose = (OtpPurpose) session.getAttribute(OTP_PURPOSE);
        if (email == null || purpose == null) {
            // Phiên đã hết hạn hoặc vào thẳng URL: quay lại Login
            redirect.addFlashAttribute("error", ErrorCode.AUTH_VERIFICATION_FAILED.getMessage());
            return "redirect:/login";
        }
        // 6 ô, mỗi ô một chữ số -> ghép lại thành mã 6 số
        String code = digits == null ? "" : String.join("", digits).trim();
        try {
            if (purpose == OtpPurpose.VERIFY_EMAIL) {
                authService.verifyAndRegister(email, code);
                clearOtp(session);
                // Đúng mã đăng ký: tài khoản AWAITING_APPROVAL, hiện thông báo chờ duyệt rồi về Login
                redirect.addFlashAttribute("popup", "awaiting");
            } else {
                authService.verifyResetCode(email, code);
                clearOtp(session);
                // Đúng mã quên mật khẩu: cho phép mở pop-up Đặt lại mật khẩu
                session.setAttribute(RESET_EMAIL, email);
                redirect.addFlashAttribute("popup", "reset");
            }
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("popup", "otp");
            redirect.addFlashAttribute("fieldErrors", Map.of("code", ex.getErrorCode().getMessage()));
        } catch (Exception ex) {
            log.error("OTP verify failed for {}", email, ex);
            redirect.addFlashAttribute("popup", "otp");
            redirect.addFlashAttribute("fieldErrors", Map.of("code", ErrorCode.AUTH_UNEXPECTED_ERROR.getMessage()));
        }
        return "redirect:/login";
    }

    @PostMapping("/otp/resend")
    public String resendOtp(HttpSession session, RedirectAttributes redirect) {
        String email = (String) session.getAttribute(OTP_EMAIL);
        OtpPurpose purpose = (OtpPurpose) session.getAttribute(OTP_PURPOSE);
        if (email == null || purpose == null) return "redirect:/login";
        redirect.addFlashAttribute("popup", "otp");
        // Chặn bấm gửi lại trước 60 giây (giao diện cũng khóa nút, đây là kiểm tra ở backend)
        if (resendWaitSeconds(session) > 0) {
            redirect.addFlashAttribute("error", ErrorCode.AUTH_OTP_RESEND_TOO_SOON.getMessage());
            return "redirect:/login";
        }
        try {
            authService.resendCode(email, purpose);
            session.setAttribute(OTP_SENT_AT, System.currentTimeMillis());
            redirect.addFlashAttribute("message", "Mã mới đã được gửi. Mã cũ không còn hiệu lực.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Resend OTP failed for {}", email, ex);
            redirect.addFlashAttribute("error", ErrorCode.AUTH_UNEXPECTED_ERROR.getMessage());
        }
        return "redirect:/login";
    }

    // ===================== Quên / đặt lại mật khẩu =====================

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam(required = false) String email, HttpSession session,
                                 RedirectAttributes redirect) {
        try {
            authService.sendPasswordResetCode(email);
        } catch (ApplicationException ex) {
            // Chỉ lỗi định dạng email mới báo lại; email có tồn tại hay không thì không tiết lộ
            redirect.addFlashAttribute("popup", "forgot");
            redirect.addFlashAttribute("email", email);
            redirect.addFlashAttribute("fieldErrors", Map.of("email", ex.getErrorCode().getMessage()));
            return "redirect:/login";
        } catch (Exception ex) {
            log.error("Forgot password failed for {}", email, ex);
        }
        // Luôn mở pop-up OTP với CÙNG một thông báo, dù email có tồn tại hay không
        startOtp(session, email.trim().toLowerCase(), OtpPurpose.RESET_PASSWORD, true);
        redirect.addFlashAttribute("popup", "otp");
        redirect.addFlashAttribute("message", FORGOT_SENT_MESSAGE);
        return "redirect:/login";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam(required = false) String password,
                                @RequestParam(required = false) String confirmPassword,
                                HttpSession session, RedirectAttributes redirect) {
        String email = (String) session.getAttribute(RESET_EMAIL);
        // Chưa qua bước OTP thì không được đặt lại mật khẩu
        if (email == null) {
            redirect.addFlashAttribute("error", ErrorCode.AUTH_RESET_NOT_ALLOWED.getMessage());
            return "redirect:/login";
        }
        Map<String, String> errors = authService.validateNewPassword(password, confirmPassword);
        if (!errors.isEmpty()) {
            redirect.addFlashAttribute("popup", "reset");
            redirect.addFlashAttribute("fieldErrors", errors);
            return "redirect:/login";
        }
        try {
            authService.resetPassword(email, password, confirmPassword);
            session.removeAttribute(RESET_EMAIL);
            redirect.addFlashAttribute("message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.");
            redirect.addFlashAttribute("email", email);
        } catch (ApplicationException ex) {
            session.removeAttribute(RESET_EMAIL);
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        }
        return "redirect:/login";
    }

    // ===================== Hàm phụ =====================

    // Ghi nhớ email + mục đích đang chờ OTP. justSent = true: vừa gửi mã nên bắt đầu đếm ngược 60 giây
    private void startOtp(HttpSession session, String email, OtpPurpose purpose, boolean justSent) {
        session.removeAttribute(RESET_EMAIL);
        session.setAttribute(OTP_EMAIL, email);
        session.setAttribute(OTP_PURPOSE, purpose);
        if (justSent) {
            session.setAttribute(OTP_SENT_AT, System.currentTimeMillis());
        } else {
            // Mã có thể đã gửi từ trước (đăng nhập lại khi PENDING): hỏi service còn phải chờ bao lâu
            int wait = authService.getResendWaitSeconds(email, purpose);
            session.setAttribute(OTP_SENT_AT, System.currentTimeMillis() - (RESEND_SECONDS - wait) * 1000L);
        }
    }

    private void clearOtp(HttpSession session) {
        session.removeAttribute(OTP_EMAIL);
        session.removeAttribute(OTP_PURPOSE);
        session.removeAttribute(OTP_SENT_AT);
    }

    // Số giây còn phải chờ trước khi bấm "Gửi lại mã" (tính theo lần gửi gần nhất lưu trong session)
    private static int resendWaitSeconds(HttpSession session) {
        Object sentAt = session.getAttribute(OTP_SENT_AT);
        if (!(sentAt instanceof Long time)) return 0;
        long passed = (System.currentTimeMillis() - time) / 1000;
        return (int) Math.max(0, RESEND_SECONDS - passed);
    }
}
