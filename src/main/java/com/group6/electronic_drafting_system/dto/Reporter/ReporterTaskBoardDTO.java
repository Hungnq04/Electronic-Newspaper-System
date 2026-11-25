package com.group6.electronic_drafting_system.dto.Reporter;

import java.util.List;

// Dữ liệu trang "Nhiệm vụ tác nghiệp": danh sách + thống kê + cột Kanban.
// Chỉ chứa dữ liệu, các số đếm đã được ReporterTaskServiceImpl tính sẵn.
// todoCount / doingCount / reviewCount / doneCount: số nhiệm vụ theo TaskStage.group
// openCount: số việc đang mở = todoCount + doingCount
public record ReporterTaskBoardDTO(String fullName, String departmentName, String initials,
                                   List<ReporterTaskDTO> tasks, long urgentCount,
                                   long todoCount, long doingCount, long reviewCount, long doneCount, long openCount,
                                   List<String> categories, List<Column> columns) {

    public record Column(String key, String label, List<ReporterTaskDTO> tasks) {
    }
}
