package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;

// Dữ liệu trang "Đề tài đã duyệt": thông tin Trưởng phòng + danh sách đề tài + số đếm theo tình trạng giao việc.
// Chỉ chứa dữ liệu, các số đếm đã được DepartmentHeadTopicServiceImpl tính sẵn.
// noLeadCount / leadPendingCount / assignedCount: số đề tài ở từng TopicAssignmentState
public record DepartmentHeadTopicBoardDTO(String fullName, String departmentName, String initials,
                                          List<DepartmentHeadTopicDTO> topics,
                                          long noLeadCount, long leadPendingCount, long assignedCount,
                                          List<String> categories) {
}
