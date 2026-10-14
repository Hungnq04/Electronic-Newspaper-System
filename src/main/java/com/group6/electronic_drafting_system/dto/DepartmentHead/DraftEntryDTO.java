package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// Một thẻ trong danh sách chờ duyệt của Trưởng ban: bài viết (kind = "article") hoặc đề tài (kind = "topic")
@Getter
@Setter
@Builder
public class DraftEntryDTO {
    private Integer id;
    private String kind;
    private String code;
    private String title;
    private String excerpt;
    private String category;
    private boolean urgent;
    private String authorName;
    private String authorInitials;
    private String submittedAgo;
    private String wordCount;
    private String sourceCount;
    private String deadlineText;
    private String statusText;
    // Dùng để sắp xếp: không hiển thị trực tiếp
    private LocalDate expectedDate;
    private OffsetDateTime submittedAt;
    // Thứ tự (bắt đầu từ 1) cho nút sắp xếp phía JS: data-deadline-order / data-created-order
    private int deadlineOrder;
    private int createdOrder;
}
