package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.DepartmentHead.DepartmentHeadDashboardDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewHistoryPageDTO;

// Interface = "bản hợp đồng": chỉ ghi tên các việc service này làm. Code thật nằm ở impl/DepartmentHeadServiceImpl.java
public interface DepartmentHeadService {
    // Màn dashboard: bài + đề tài chờ duyệt, 4 hộp thống kê
    DepartmentHeadDashboardDTO getDashboard(String email);

    // Màn "Lịch sử duyệt" (UC-35): các lượt duyệt cấp phòng mà chính Trưởng ban này đã chốt
    ReviewHistoryPageDTO getReviewHistory(String email);
}
