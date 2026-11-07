package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// Dữ liệu trang "Giao việc / Quản lý nhóm" của một đề tài.
// topic: tóm tắt đề tài (dùng lại DTO của danh sách); lead = null khi đề tài chưa có Lead.
// supporters: Supporting đang có trên đề tài; candidates: phóng viên ACTIVE chưa tham gia đề tài.
// editable = true: đề tài chưa giao xong -> hiện form giao việc lần đầu (FL-03).
// editable = false: đã giao xong -> hiện phần quản lý nhóm (thêm / gỡ Supporting, đổi Lead, lịch sử).
// leadOptions: người có thể nhận Lead mới (ứng viên + Supporting đang làm);
// blockingArticleCount: số bài của Lead hiện tại đang trong quy trình duyệt -> > 0 thì không cho đổi Lead.
public record TopicAssignmentPageDTO(String fullName, String departmentName, String initials,
                                     DepartmentHeadTopicDTO topic, String rationale, LocalDate expectedDate,
                                     Person lead, OffsetDateTime leadAssignedAt, OffsetDateTime leadDeadline, String leadRequirements,
                                     List<Supporter> supporters, List<Person> candidates, boolean editable,
                                     List<LeadOption> leadOptions, long blockingArticleCount, List<HistoryItem> history) {

    public boolean hasLead() { return lead != null; }

    public boolean canChangeLead() { return blockingArticleCount == 0 && !leadOptions.isEmpty(); }

    // sameDepartment: cùng phòng ban với Trưởng phòng đang giao việc (được xếp lên đầu danh sách)
    public record Person(Integer accountId, String name, String initials, String email, String departmentName, boolean sameDepartment) {
    }

    // removable = false khi Supporting đã COMPLETED (không cho gỡ)
    public record Supporter(Integer assignmentId, String name, String initials, String departmentName,
                            OffsetDateTime assignedAt, OffsetDateTime deadline, String requirements,
                            String statusLabel, boolean removable) {
    }

    // currentSupporter = true: người này đang là Supporting, nhận Lead thì dòng Supporting của họ bị đóng
    public record LeadOption(Integer accountId, String name, String departmentName, boolean currentSupporter) {
    }

    // Một dòng assignment đã REMOVED (bị gỡ / bị thay)
    public record HistoryItem(String name, String initials, String roleLabel, OffsetDateTime assignedAt,
                              OffsetDateTime removedAt, String removedByName) {
    }
}
