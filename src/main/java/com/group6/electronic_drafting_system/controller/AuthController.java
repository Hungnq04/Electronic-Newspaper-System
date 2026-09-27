package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, @RequestParam(required = false) String logout, Model model) {
        if (error != null) model.addAttribute("error", "Email hoặc mật khẩu không đúng.");
        if (logout != null) model.addAttribute("message", "Bạn đã đăng xuất.");
        return "login";
    }

    @GetMapping("/register")
    public String register() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String email, @RequestParam String password,
                          RedirectAttributes redirect) {
        try {
            authService.sendRegistrationCode(fullName, email, password);
            redirect.addFlashAttribute("message", "OTP đã được gửi. Mã có hiệu lực trong 60 giây.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", safeMessage(ex));
            redirect.addFlashAttribute("fullName", fullName);
            redirect.addFlashAttribute("email", email);
            return "redirect:/register";
        }
        return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }

    @GetMapping("/register/verify")
    public String verifyPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/register/verify")
    public String verify(@RequestParam String email, @RequestParam String otp, RedirectAttributes redirect) {
        try {
            authService.verifyAndRegister(email, otp);
            redirect.addFlashAttribute("message", "Xác minh thành công. Bạn có thể đăng nhập.");
            return "redirect:/login";
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", safeMessage(ex));
            return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/register/resend-otp")
    public String resendOtp(@RequestParam String email, RedirectAttributes redirect) {
        try {
            authService.resendRegistrationCode(email);
            redirect.addFlashAttribute("message", "OTP mới đã được gửi. Mã cũ không còn hiệu lực.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", safeMessage(ex));
        }
        return "redirect:/register/verify?email=" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private String safeMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && !(cause instanceof IllegalArgumentException)) cause = cause.getCause();
        return cause.getMessage() == null ? "Không thể xử lý yêu cầu. Kiểm tra cấu hình email và thử lại." : cause.getMessage();
    }
}
