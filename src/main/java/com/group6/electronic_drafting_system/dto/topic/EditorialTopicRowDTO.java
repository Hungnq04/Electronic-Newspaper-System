package com.group6.electronic_drafting_system.dto.topic;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

// Một dòng trong Editorial Topic List
@Value @Builder
public class EditorialTopicRowDTO {
    Integer id;
    String title;
    String category;
    String priority;
    String priorityLabel;
    LocalDate expectedDate;
    String creatorName;
    // true: Topic top-down do Thư ký tạo; false: Reporter đề xuất đã được duyệt
    boolean editorial;
    OffsetDateTime createdAt;
    String departmentHeadName;
    String departmentName;
    OffsetDateTime assignedAt;
    String leadName;
    long supportingCount;
    long articleCount;
}
