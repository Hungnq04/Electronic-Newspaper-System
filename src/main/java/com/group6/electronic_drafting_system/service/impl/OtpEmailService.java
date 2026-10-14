package com.group6.electronic_drafting_system.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OtpEmailService {
    private final JavaMailSender mailSender;
    private final String from;

    // Bắt buộc set From: thiếu thì JavaMail tự ghép user@hostname và lỗi "can't determine local email address"
    public OtpEmailService(JavaMailSender mailSender, @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Async("otpMailExecutor")
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Mã xác minh đăng ký Electronic Drafting System");
        message.setText("Mã OTP của bạn là " + code + ". Mã hết hạn sau 60 giây. Không chia sẻ mã này với bất kỳ ai.");
        mailSender.send(message);
    }
}
