package com.group6.electronic_drafting_system.entity.enums;

// AWAITING_APPROVAL: đã xác minh email (OTP hoặc Google), chờ Admin duyệt và cấp vai trò; chưa đăng nhập được
public enum AccountStatus {
    PENDING, AWAITING_APPROVAL, ACTIVE, LOCKED, INACTIVE
}
