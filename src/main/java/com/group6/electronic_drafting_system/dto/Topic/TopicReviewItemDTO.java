package com.group6.electronic_drafting_system.dto.Topic;

import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.time.OffsetDateTime;

// Một đề tài trên màn duyệt của Trưởng ban / Thư ký (dòng trong hàng chờ và phần chi tiết)
// revisionRounds: số lần Trưởng ban đã yêu cầu sửa
public record TopicReviewItemDTO(Integer id, String code, String title, String summary, String departmentName,
                                 String reporterName, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                 int revisionRounds, TopicStatus status, boolean awaitingRevision) {

    public String statusLabel() { return TopicListItemDTO.labelOf(status); }

    // Hậu tố class CSS của badge: tp-badge--department-review ...
    public String statusKey() { return TopicListItemDTO.keyOf(status); }
}
