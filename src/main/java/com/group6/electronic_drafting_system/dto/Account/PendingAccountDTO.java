package com.group6.electronic_drafting_system.dto.Account;

import java.time.OffsetDateTime;

// Một dòng trong danh sách tài khoản chờ Admin duyệt (màn Admin dùng)
public record PendingAccountDTO(Integer id, String fullName, String email, OffsetDateTime registeredAt) {}
