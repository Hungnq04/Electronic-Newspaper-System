package com.group6.electronic_drafting_system.dto.DepartmentHead;

// Tình trạng giao việc của một đề tài, nhìn từ phía Trưởng phòng.
// Không lưu trong DB (CLAUDE.md mục 4.3): suy ra từ assignment LEAD của đề tài.
// Thứ tự khai báo = thứ tự ưu tiên xử lý (việc cần làm trước xếp lên trên).
public enum TopicAssignmentState {
    NO_LEAD("Chưa có Lead"),               // Đề tài Thư ký giao xuống: Trưởng phòng phải chọn Lead
    LEAD_PENDING("Chờ giao nhiệm vụ"),     // Đã có Lead (tự tạo khi duyệt đề xuất) nhưng chưa có nhiệm vụ / deadline
    ASSIGNED("Đã giao");                   // Lead đã có đủ nhiệm vụ và deadline

    private final String label;

    TopicAssignmentState(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
