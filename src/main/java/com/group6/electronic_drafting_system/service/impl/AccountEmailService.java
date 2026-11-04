package com.group6.electronic_drafting_system.service.impl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// Email báo kết quả Admin duyệt đăng ký. Gửi nền như mail OTP để không chặn thao tác của Admin.
@Service
public class AccountEmailService {
    private static final String SUBJECT = "Kết quả duyệt tài khoản Electronic Drafting System";
    private final JavaMailSender mailSender;

    public AccountEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("otpMailExecutor")
    public void sendApproved(String email, String fullName, String roleLabel, String departmentName) {
        String scope = departmentName == null ? roleLabel : roleLabel + " – " + departmentName;
        send(email, "Xin chào " + fullName + ",\n\n"
                + "Tài khoản của bạn đã được Quản trị viên duyệt với vai trò: " + scope + ".\n"
                + "Bạn có thể đăng nhập vào hệ thống bằng email này.\n");
    }

    @Async("otpMailExecutor")
    public void sendRejected(String email, String fullName, String reason) {
        send(email, "Xin chào " + fullName + ",\n\n"
                + "Yêu cầu đăng ký tài khoản của bạn không được duyệt."
                + (reason == null || reason.isBlank() ? "" : "\nLý do: " + reason.trim())
                + "\nVui lòng liên hệ Quản trị viên nếu cần hỗ trợ.\n");
    }

    private void send(String email, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText(text);
        mailSender.send(message);
    }
}
