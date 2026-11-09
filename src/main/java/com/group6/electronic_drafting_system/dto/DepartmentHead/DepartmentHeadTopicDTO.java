package com.group6.electronic_drafting_system.dto.DepartmentHead;

import com.group6.electronic_drafting_system.entity.enums.Priority;
import java.time.OffsetDateTime;
import java.util.List;

// Một dòng trong danh sách "Đề tài đã duyệt" của Trưởng phòng.
// editorial = true: đề tài do Thư ký tạo và giao xuống (không qua duyệt), approvedAt lúc đó là ngày tạo đề tài.
// lead = null khi đề tài chưa có Lead; leadDeadline / deadlineText là hạn của Lead.
public record DepartmentHeadTopicDTO(Integer topicId, String title, String description, String category,
                                     Priority priority, String priorityLabel,
                                     String proposerName, boolean editorial, String approvedByName, OffsetDateTime approvedAt,
                                     Member lead, List<Member> supporters,
                                     OffsetDateTime leadDeadline, String deadlineText, boolean overdue,
                                     TopicAssignmentState state) {

    // Số người đang tham gia (Lead + Supporting), không tính Trưởng phòng
    public int teamSize() { return (lead == null ? 0 : 1) + supporters.size(); }

    // Chuỗi để ô tìm kiếm phía trình duyệt so khớp: tên đề tài, mã, chuyên mục, người đề xuất, Lead
    public String searchText() {
        String text = title + " #" + topicId + " " + category + " " + proposerName + " " + (lead == null ? "" : lead.name());
        return text.toLowerCase();
    }

    public record Member(String name, String initials) {
    }
}
