package com.group6.electronic_drafting_system.dto.Reporter;

import java.util.List;
import java.util.Map;

// Dữ liệu trang "Nhiệm vụ tác nghiệp": danh sách + thống kê + cột Kanban.
// groupCounts: số nhiệm vụ theo TaskStage.group (todo / doing / review / done)
public record ReporterTaskBoardDTO(String fullName, String departmentName, String initials,
                                   List<ReporterTaskDTO> tasks, long urgentCount, Map<String, Long> groupCounts,
                                   List<String> categories, List<Column> columns) {

    public long count(String group) { return groupCounts.getOrDefault(group, 0L); }

    public record Column(String key, String label, List<ReporterTaskDTO> tasks) {
    }
}
