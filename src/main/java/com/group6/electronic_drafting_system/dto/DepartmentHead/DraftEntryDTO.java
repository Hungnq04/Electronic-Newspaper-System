package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// Một dòng trong bảng chờ duyệt của Trưởng ban (dashboard chỉ hiện bài viết, kind = "article")
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
    // Dùng để sắp xếp bảng ở service: không hiển thị trực tiếp
    private LocalDate expectedDate;
    private OffsetDateTime submittedAt;
}
