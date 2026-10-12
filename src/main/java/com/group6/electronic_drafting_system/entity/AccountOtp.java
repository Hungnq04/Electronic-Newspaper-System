package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

// Chỉ lưu mã băm; hết hạn 10 phút sau last_sent_at, tối đa 5 lần nhập sai, dùng một lần.
@Entity @Table(name = "account_otp", schema = "dbo") @Getter @Setter
public class AccountOtp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "otp_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false) private Account account;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private OtpPurpose purpose;
    @Column(name = "code_hash", nullable = false, length = 100) private String codeHash;
    @Column(name = "attempt_count", nullable = false) private Integer attemptCount = 0;
    @Column(name = "last_sent_at", nullable = false) private OffsetDateTime lastSentAt;
    @Column(name = "expires_at", nullable = false) private OffsetDateTime expiresAt;
    @Column(name = "used_at") private OffsetDateTime usedAt;
}
