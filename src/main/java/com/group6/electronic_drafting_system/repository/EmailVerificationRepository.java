package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> { }
