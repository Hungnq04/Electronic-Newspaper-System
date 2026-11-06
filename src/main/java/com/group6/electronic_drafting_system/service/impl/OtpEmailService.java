package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.entity.enums.OtpPurpose;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OtpEmailService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OtpEmailService.class);
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    // Bắt buộc set From: thiếu thì JavaMail tự ghép user@hostname và lỗi "can't determine local email address".
    // Dev: chưa cấu hình SMTP (spring.mail.username trống) thì app vẫn chạy, OTP được in ra console.
    public OtpEmailService(ObjectProvider<JavaMailSender> mailSender, @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Async("otpMailExecutor")
    public void sendVerificationCode(String email, String code, OtpPurpose purpose) {
        String subject = purpose == OtpPurpose.RESET_PASSWORD
                ? "Mã đặt lại mật khẩu Electronic Drafting System"
                : "Mã xác minh đăng ký Electronic Drafting System";
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from == null || from.isBlank()) {
            log.warn("[DEV] Chưa cấu hình SMTP - OTP {} cho {} là: {}", purpose, email, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText("Mã OTP của bạn là " + code + ". Mã hết hạn sau 5 phút. Không chia sẻ mã này với bất kỳ ai.");
        sender.send(message);
    }
}
