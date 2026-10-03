package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.service.AuthService;
import com.group6.electronic_drafting_system.dto.Account.LoginRequestDTO;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, @RequestParam(required = false) String logout, Model model) {
        if (error != null) model.addAttribute("error", ErrorCode.fromName(error).getMessage());
        if (logout != null) model.addAttribute("message", "Bạn đã đăng xuất.");
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequestDTO loginRequest, jakarta.servlet.http.HttpServletRequest request,
                        RedirectAttributes redirect) {
        try {
            var account = authService.login(loginRequest.getEmail(), loginRequest.getPassword());
            if (!account.isVerified()) {
                redirect.addFlashAttribute("message", ErrorCode.AUTH_ACCOUNT_NOT_VERIFIED.getMessage()
                        + " Mã xác thực mới đã được gửi đến email của bạn.");
                return "redirect:/register/verify?email="
                        + java.net.URLEncoder.encode(account.getEmail(), java.nio.charset.StandardCharsets.UTF_8);
            }
            authService.persistSecurityContext(request);
            return "redirect:/";
        } catch (AuthenticationException | ApplicationException ex) {
            redirect.addFlashAttribute("error", ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage());
            return "redirect:/login?error=" + ErrorCode.AUTH_INVALID_CREDENTIALS.name();
        }
    }

    @GetMapping("/register")
    public String register() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String email, @RequestParam String password,
                          RedirectAttributes redirect) {
        try {
            authService.sendRegistrationCode(fullName, email, password);
            redirect.addFlashAttribute("message", "OTP đã được gửi. Mã có hiệu lực trong 60 giây.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            redirect.addFlashAttribute("fullName", fullName);
            redirect.addFlashAttribute("email", email);
            return "redirect:/register";
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ErrorCode.AUTH_UNEXPECTED_ERROR.getMessage());
            redirect.addFlashAttribute("fullName", fullName);
            redirect.addFlashAttribute("email", email);
            return "redirect:/register";
        }
        return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }

    @GetMapping("/register/verify")
    public String verifyPage(@RequestParam String email, @RequestParam(required = false) String pending, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("otpRemainingSeconds", authService.getOtpRemainingSeconds(email));
        if (pending != null) model.addAttribute("message", ErrorCode.AUTH_ACCOUNT_NOT_VERIFIED.getMessage()
                + " Mã xác thực đã được gửi đến email của bạn.");
        return "verify-otp";
    }

    @PostMapping("/register/verify")
    public String verify(@RequestParam String email, @RequestParam String otp, RedirectAttributes redirect) {
        try {
            authService.verifyAndRegister(email, otp);
            redirect.addFlashAttribute("message", "Xác minh thành công. Bạn có thể đăng nhập.");
            return "redirect:/login";
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ErrorCode.AUTH_UNEXPECTED_ERROR.getMessage());
            return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/register/resend-otp")
    public String resendOtp(@RequestParam String email, RedirectAttributes redirect) {
        try {
            authService.resendRegistrationCode(email);
            redirect.addFlashAttribute("message", "OTP mới đã được gửi. Mã cũ không còn hiệu lực.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ErrorCode.AUTH_UNEXPECTED_ERROR.getMessage());
        }
        return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }

}
