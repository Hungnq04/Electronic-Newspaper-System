package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// Toàn bộ dữ liệu màn "Lịch sử phiên bản" (UC-23, FL-05) của Trưởng phòng.
// TẠO Ở ĐÂU: DepartmentHeadReviewServiceImpl.getVersionHistory()
// HTML ĐỌC:  ${versionHistory...} trong department_head/version-history.html
@Getter
@Builder
public class VersionHistoryPageDTO {
    private Integer articleId;          // ghép URL quay lại màn thẩm định / mở so sánh
    private String articleCode;         // "BT-15"
    private String articleTitle;        // tít bài
    private String topicTitle;
    private String category;
    private String leadName;
    private String statusText;          // trạng thái bài hiện tại, chữ tiếng Việt
    // Các phiên bản, MỚI NHẤT TRƯỚC. Dùng lại VersionViewDTO của màn thẩm định
    // (đã có nhãn, ngày nộp, ghi chú, tít, sapo, các đoạn) + 2 ô reviewNotes, commentCount.
    private List<VersionViewDTO> versions;
    // 3 ô cho thanh trên cùng (fragment dhTopbar)
    private String displayName;
    private String departmentName;
    private String userInitials;
}