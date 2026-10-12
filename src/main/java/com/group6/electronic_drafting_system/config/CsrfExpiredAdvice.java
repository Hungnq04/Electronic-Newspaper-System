package com.group6.electronic_drafting_system.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Hiển thị thông báo một lần sau khi SecurityConfig chuyển hướng vì token CSRF hết hạn
@ControllerAdvice
public class CsrfExpiredAdvice {
    @ModelAttribute
    public void addCsrfExpiredMessage(HttpSession session, Model model) {
        if (session.getAttribute(SecurityConfig.CSRF_EXPIRED_ATTR) != null) {
            session.removeAttribute(SecurityConfig.CSRF_EXPIRED_ATTR);
            model.addAttribute("error", "Phiên làm việc đã hết hạn. Vui lòng thử lại.");
        }
    }
}
