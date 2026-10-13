package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentHeadDashboardDTO {
    private String displayName;
    private String userInitials;
    // null khi Trưởng ban chưa được gán phòng ban
    private String departmentName;
    private List<DashboardStatDTO> stats;
    private List<DraftEntryDTO> entries;
    private List<String> categories;
    private long articleCount;
    private long topicCount;
    private long urgentCount;
}
