package com.group6.electronic_drafting_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Bật chức năng chạy việc định kỳ (@Scheduled) của Spring.
// Hiện dùng cho job tự đăng bài hẹn giờ (FL-07, xem job/ScheduledPublicationJob).
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
