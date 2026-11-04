package com.group6.electronic_drafting_system.dto.Topic;

import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.time.OffsetDateTime;

// Một thẻ đề tài trên Topic Proposal List. code: mã hiển thị dạng TP-2026-0012
public record TopicListItemDTO(Integer id, String code, String title, String summary, String departmentName,
                               TopicStatus status, boolean awaitingRevision, OffsetDateTime createdAt) {

    public String statusLabel() { return labelOf(status); }

    // Hậu tố class CSS của badge / viền thẻ: tp-status--department-review ...
    public String statusKey() { return keyOf(status); }

    public static String labelOf(TopicStatus status) {
        return switch (status) {
            case DEPARTMENT_REVIEW -> "Chờ Trưởng ban duyệt";
            case SECRETARY_REVIEW -> "Chờ Thư ký duyệt";
            case ACTIVE -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
        };
    }

    public static String keyOf(TopicStatus status) {
        return status.name().toLowerCase().replace('_', '-');
    }
}
