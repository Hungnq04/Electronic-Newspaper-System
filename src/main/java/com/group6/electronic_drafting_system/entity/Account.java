package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "ACCOUNT", schema = "dbo") @Getter @Setter
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "account_id") private Integer id;
    @Column(nullable = false, unique = true, length = 50) private String username;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 100) private String fullName;
    @Column(nullable = false, unique = true, length = 100) private String email;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "role_id", nullable = false) private Role role;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id") private Department department;
    @Column(nullable = false, length = 20) private String status = "Active";
    @Column(name = "verified") private Boolean verified = false;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false) private LocalDateTime updatedAt;
}
