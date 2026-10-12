package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.AccountOtp;
import com.group6.electronic_drafting_system.entity.enums.OtpPurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountOtpRepository extends JpaRepository<AccountOtp, Integer> {
    // Mã gần nhất chưa dùng của tài khoản cho một mục đích
    Optional<AccountOtp> findFirstByAccountAndPurposeAndUsedAtIsNullOrderByLastSentAtDescIdDesc(Account account, OtpPurpose purpose);
}
