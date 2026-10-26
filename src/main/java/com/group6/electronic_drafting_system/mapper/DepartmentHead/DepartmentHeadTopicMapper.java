package com.group6.electronic_drafting_system.mapper.DepartmentHead;

import com.group6.electronic_drafting_system.dto.DepartmentHead.DepartmentHeadTopicDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentPageDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentState;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.TopicReview;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.mapper.Reporter.ReporterTaskMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DepartmentHeadTopicMapper {
    // Thời gian trong hệ thống dùng giờ Việt Nam (CLAUDE.md mục 8)
    public static final ZoneOffset VN_OFFSET = ZoneOffset.ofHours(7);
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // team: các assignment chưa REMOVED của đề tài; secretaryReview: kết quả duyệt cấp Thư ký (null nếu là đề tài Thư ký tự tạo)
    public DepartmentHeadTopicDTO toTopicDTO(Topic topic, Collection<Assignment> team, TopicReview secretaryReview, OffsetDateTime now) {
        Assignment lead = team.stream().filter(a -> a.getAssignmentRole() == AssignmentRole.LEAD).findFirst().orElse(null);
        List<DepartmentHeadTopicDTO.Member> supporters = team.stream()
                .filter(a -> a.getAssignmentRole() == AssignmentRole.SUPPORTING)
                .map(a -> toMember(a.getAccount()))
                .toList();

        // Đề tài Thư ký giao xuống không qua duyệt (BR-05): lấy ngày tạo làm "ngày duyệt"
        boolean editorial = secretaryReview == null;
        OffsetDateTime approvedAt = editorial ? topic.getCreatedAt() : secretaryReview.getReviewedAt();
        String approvedByName = editorial ? null : secretaryReview.getReviewer().getFullName();

        OffsetDateTime deadline = lead == null ? null : lead.getDeadline();
        boolean overdue = deadline != null && deadline.isBefore(now);

        return new DepartmentHeadTopicDTO(topic.getId(), topic.getTitle(), topic.getDescription(), topic.getCategory(),
                topic.getPriority(), ReporterTaskMapper.priorityLabel(topic.getPriority()),
                topic.getCreator().getFullName(), editorial, approvedByName, approvedAt,
                lead == null ? null : toMember(lead.getAccount()), supporters,
                deadline, deadlineText(deadline, now), overdue,
                stateOf(lead));
    }

    // 3 mức giao việc, suy ra từ assignment LEAD (CLAUDE.md mục 4.3)
    public static TopicAssignmentState stateOf(Assignment lead) {
        if (lead == null) return TopicAssignmentState.NO_LEAD;
        boolean noRequirements = lead.getRequirements() == null || lead.getRequirements().isBlank();
        if (lead.getDeadline() == null || noRequirements) return TopicAssignmentState.LEAD_PENDING;
        return TopicAssignmentState.ASSIGNED;
    }

    // "Còn 2 ngày 3 giờ" / "Quá hạn 5 giờ 10 phút"
    static String deadlineText(OffsetDateTime deadline, OffsetDateTime now) {
        if (deadline == null) return null;
        Duration left = Duration.between(now, deadline);
        return (left.isNegative() ? "Quá hạn " : "Còn ") + ReporterTaskMapper.formatDuration(left.abs());
    }

    private static DepartmentHeadTopicDTO.Member toMember(Account account) {
        return new DepartmentHeadTopicDTO.Member(account.getFullName(), ReporterTaskMapper.initials(account.getFullName()));
    }

    // ===== Trang "Giao việc" =====

    // myDepartmentId: phòng ban của Trưởng phòng, để đánh dấu phóng viên cùng phòng
    public TopicAssignmentPageDTO.Person toPerson(Account account, Integer myDepartmentId) {
        Department department = account.getDepartment();
        boolean sameDepartment = department != null && department.getId().equals(myDepartmentId);
        return new TopicAssignmentPageDTO.Person(account.getId(), account.getFullName(), ReporterTaskMapper.initials(account.getFullName()),
                account.getEmail(), department == null ? null : department.getDepartmentName(), sameDepartment);
    }

    public TopicAssignmentPageDTO.Supporter toSupporter(Assignment assignment) {
        Account account = assignment.getAccount();
        boolean completed = assignment.getStatus() == AssignmentStatus.COMPLETED;
        return new TopicAssignmentPageDTO.Supporter(assignment.getId(), account.getFullName(), ReporterTaskMapper.initials(account.getFullName()),
                departmentName(account), assignment.getAssignedAt(), assignment.getDeadline(), assignment.getRequirements(),
                completed ? "Đã hoàn thành" : "Đang thực hiện",
                !completed);   // Supporting đã COMPLETED thì không cho gỡ
    }

    public TopicAssignmentPageDTO.LeadOption toLeadOption(Account account, boolean currentSupporter) {
        return new TopicAssignmentPageDTO.LeadOption(account.getId(), account.getFullName(), departmentName(account), currentSupporter);
    }

    public TopicAssignmentPageDTO.HistoryItem toHistoryItem(Assignment removed) {
        Account account = removed.getAccount();
        return new TopicAssignmentPageDTO.HistoryItem(account.getFullName(), ReporterTaskMapper.initials(account.getFullName()),
                shortRoleLabel(removed.getAssignmentRole()), removed.getAssignedAt(), removed.getRemovedAt(),
                removed.getRemovedBy() == null ? null : removed.getRemovedBy().getFullName());
    }

    static String shortRoleLabel(AssignmentRole role) {
        return switch (role) {
            case LEAD -> "Lead";
            case SUPPORTING -> "Supporting";
            case DEPARTMENT_HEAD -> "Trưởng phòng";
        };
    }

    private static String departmentName(Account account) {
        return account.getDepartment() == null ? null : account.getDepartment().getDepartmentName();
    }

    // OffsetDateTime -> giá trị cho <input type="datetime-local"> theo giờ Việt Nam, ví dụ "2026-10-30T17:00"
    public static String toInputValue(OffsetDateTime time) {
        if (time == null) return null;
        return time.withOffsetSameInstant(VN_OFFSET).toLocalDateTime().format(INPUT_FORMAT);
    }

    // Chiều ngược lại: chuỗi từ ô nhập (giờ Việt Nam) -> OffsetDateTime +07:00. Sai định dạng thì ném DateTimeParseException.
    public static OffsetDateTime fromInputValue(String value) {
        return LocalDateTime.parse(value.trim()).atOffset(VN_OFFSET).truncatedTo(ChronoUnit.SECONDS);
    }
}
