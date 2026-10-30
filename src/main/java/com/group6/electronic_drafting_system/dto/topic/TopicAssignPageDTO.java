package com.group6.electronic_drafting_system.dto.topic;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

// Dữ liệu màn Assign Topic to Department Head (dùng cả khi giao lại)
@Value @Builder
public class TopicAssignPageDTO {
    Integer topicId;
    String title;
    String description;
    String category;
    String rationale;
    String priorityLabel;
    LocalDate expectedDate;
    String creatorName;
    OffsetDateTime createdAt;
    // Trưởng phòng đang phụ trách (null nếu Topic chưa giao)
    Integer currentHeadId;
    String currentHeadName;
    OffsetDateTime currentAssignedAt;
    List<String> teamSummary;
    List<HeadOption> departmentHeads;

    @Value
    public static class HeadOption {
        Integer id;
        String fullName;
        String departmentName;
        long activeTopicCount;
    }
}
