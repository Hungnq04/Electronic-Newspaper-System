package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "EMAIL_VERIFICATION", schema = "dbo")
@Getter @Setter
public class EmailVerification {
    @Id @Column(length = 100) private String email;
    @Column(name = "code_hash", nullable = false, length = 100) private String codeHash;
    @Column(name = "full_name", nullable = false, length = 100) private String fullName;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
}
