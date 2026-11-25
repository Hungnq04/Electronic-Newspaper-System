package com.group6.electronic_drafting_system.mapper.DepartmentHead;

import com.group6.electronic_drafting_system.dto.DepartmentHead.DepartmentHeadTopicDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentPageDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentState;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Topic;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

// Chỉ chép dữ liệu từ entity sang DTO. Các giá trị cần tính (Lead, trạng thái giao việc, deadline, nhãn...)
// do DepartmentHeadTopicServiceImpl tính rồi truyền vào.
@Component
public class DepartmentHeadTopicMapper {

    public DepartmentHeadTopicDTO toTopicDTO(Topic topic, String priorityLabel, boolean editorial, String approvedByName,
                                             OffsetDateTime approvedAt, DepartmentHeadTopicDTO.Member lead,
                                             List<DepartmentHeadTopicDTO.Member> supporters, OffsetDateTime leadDeadline,
                                             String deadlineText, boolean overdue, TopicAssignmentState state,
                                             int teamSize, String searchText) {
        return new DepartmentHeadTopicDTO(topic.getId(), topic.getTitle(), topic.getDescription(), topic.getCategory(),
                topic.getPriority(), priorityLabel,
                topic.getCreator().getFullName(), editorial, approvedByName, approvedAt,
                lead, supporters,
                leadDeadline, deadlineText, overdue,
                state, teamSize, searchText);
    }

    public DepartmentHeadTopicDTO.Member toMember(Account account, String initials) {
        return new DepartmentHeadTopicDTO.Member(account.getFullName(), initials);
    }

    // ===== Trang "Giao việc" =====

    public TopicAssignmentPageDTO.Person toPerson(Account account, String initials, String departmentName, boolean sameDepartment) {
        return new TopicAssignmentPageDTO.Person(account.getId(), account.getFullName(), initials,
                account.getEmail(), departmentName, sameDepartment);
    }

    public TopicAssignmentPageDTO.Supporter toSupporter(Assignment assignment, String initials, String departmentName,
                                                        String statusLabel, boolean removable) {
        return new TopicAssignmentPageDTO.Supporter(assignment.getId(), assignment.getAccount().getFullName(), initials,
                departmentName, assignment.getAssignedAt(), assignment.getDeadline(), assignment.getRequirements(),
                statusLabel, removable);
    }

    public TopicAssignmentPageDTO.LeadOption toLeadOption(Account account, String departmentName, boolean currentSupporter) {
        return new TopicAssignmentPageDTO.LeadOption(account.getId(), account.getFullName(), departmentName, currentSupporter);
    }

    public TopicAssignmentPageDTO.HistoryItem toHistoryItem(Assignment removed, String initials, String roleLabel, String removedByName) {
        return new TopicAssignmentPageDTO.HistoryItem(removed.getAccount().getFullName(), initials,
                roleLabel, removed.getAssignedAt(), removed.getRemovedAt(), removedByName);
    }
}
