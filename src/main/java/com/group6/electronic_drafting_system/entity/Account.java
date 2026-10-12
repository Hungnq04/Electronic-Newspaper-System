package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Entity @Table(name = "account", schema = "dbo") @Getter @Setter
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "account_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "role_id", nullable = false) private Role role;
    // Có thể trống: người mới đăng ký, Admin
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id") private Department department;
    @Column(nullable = false, unique = true, length = 50) private String username;
    @Column(name = "full_name", nullable = false, length = 100) private String fullName;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false, unique = true, length = 150) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AccountStatus status = AccountStatus.PENDING;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
