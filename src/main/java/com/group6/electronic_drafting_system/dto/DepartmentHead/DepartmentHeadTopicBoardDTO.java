package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import java.util.Map;

// Dữ liệu trang "Đề tài đã duyệt": thông tin Trưởng phòng + danh sách đề tài + số đếm theo tình trạng giao việc.
public record DepartmentHeadTopicBoardDTO(String fullName, String departmentName, String initials,
                                          List<DepartmentHeadTopicDTO> topics,
                                          Map<TopicAssignmentState, Long> stateCounts, List<String> categories) {

    // Dùng trong template: board.count('NO_LEAD')
    public long count(String state) {
        return stateCounts.getOrDefault(TopicAssignmentState.valueOf(state), 0L);
    }
}
