package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.topic.EditorialTopicForm;
import com.group6.electronic_drafting_system.dto.topic.EditorialTopicRowDTO;
import com.group6.electronic_drafting_system.dto.topic.TopicAssignForm;
import com.group6.electronic_drafting_system.dto.topic.TopicAssignPageDTO;
import com.group6.electronic_drafting_system.dto.topic.TopicAssignPageDTO.HeadOption;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.TopicRepository;
import com.group6.electronic_drafting_system.service.EditorialTopicService;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditorialTopicServiceImpl implements EditorialTopicService {
    private final AccountRepository accounts;
    private final TopicRepository topics;
    private final AssignmentRepository assignments;
    private final ArticleRepository articles;

    public EditorialTopicServiceImpl(AccountRepository accounts, TopicRepository topics,
                                     AssignmentRepository assignments, ArticleRepository articles) {
        this.accounts = accounts;
        this.topics = topics;
        this.assignments = assignments;
        this.articles = articles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EditorialTopicRowDTO> getUnassignedTopics(String secretaryEmail) {
        requireSecretary(secretaryEmail);
        return buildRows(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EditorialTopicRowDTO> getAssignedTopics(String secretaryEmail) {
        requireSecretary(secretaryEmail);
        return buildRows(true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategorySuggestions() {
        return topics.findDistinctCategories();
    }

    @Override
    @Transactional
    public Integer createTopic(EditorialTopicForm form, String secretaryEmail) {
        Account me = requireSecretary(secretaryEmail);
        Topic topic = new Topic();
        topic.setCreator(me);
        topic.setTitle(form.getTitle().strip());
        topic.setDescription(trimToNull(form.getDescription()));
        topic.setCategory(form.getCategory().strip());
        topic.setRationale(trimToNull(form.getRationale()));
        topic.setExpectedDate(form.getExpectedDate());
        topic.setPriority(form.getPriority() == null ? Priority.NORMAL : form.getPriority());
        // BR-05 / BR-26: Topic top-down không qua duyệt, ACTIVE ngay khi lưu và vào danh sách "Topic chưa giao"
        topic.setStatus(TopicStatus.ACTIVE);
        return topics.save(topic).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public TopicAssignPageDTO getAssignPage(Integer topicId, String secretaryEmail) {
        requireSecretary(secretaryEmail);
        Topic topic = topics.findWithCreatorById(topicId).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_NOT_FOUND));
        if (topic.getStatus() != TopicStatus.ACTIVE) throw new ApplicationException(ErrorCode.TOPIC_NOT_ACTIVE);

        List<Assignment> team = assignments.findByTopicAndStatusNot(topic, AssignmentStatus.REMOVED);
        Assignment currentHead = team.stream()
                .filter(a -> a.getAssignmentRole() == AssignmentRole.DEPARTMENT_HEAD).findFirst().orElse(null);
        Map<Integer, Long> workload = assignments.countActiveByRole(AssignmentRole.DEPARTMENT_HEAD, AssignmentStatus.REMOVED).stream()
                .collect(Collectors.toMap(r -> (Integer) r[0], r -> (Long) r[1]));

        return TopicAssignPageDTO.builder()
                .topicId(topic.getId())
                .title(topic.getTitle())
                .description(topic.getDescription())
                .category(topic.getCategory())
                .rationale(topic.getRationale())
                .priorityLabel(priorityLabel(topic.getPriority()))
                .expectedDate(topic.getExpectedDate())
                .creatorName(topic.getCreator().getFullName())
                .createdAt(topic.getCreatedAt())
                .currentHeadId(currentHead == null ? null : currentHead.getAccount().getId())
                .currentHeadName(currentHead == null ? null : currentHead.getAccount().getFullName())
                .currentAssignedAt(currentHead == null ? null : currentHead.getAssignedAt())
                .teamSummary(team.stream()
                        .filter(a -> a.getAssignmentRole() != AssignmentRole.DEPARTMENT_HEAD)
                        .sorted(Comparator.comparing(Assignment::getAssignmentRole))
                        .map(a -> a.getAccount().getFullName()
                                + (a.getAssignmentRole() == AssignmentRole.LEAD ? " (Lead Reporter)" : " (Supporting Reporter)"))
                        .toList())
                .departmentHeads(accounts.findByRoleRoleNameAndStatusOrderByFullNameAsc(RoleName.DEPARTMENT_HEAD, AccountStatus.ACTIVE)
                        .stream()
                        .map(h -> new HeadOption(h.getId(), h.getFullName(),
                                h.getDepartment() == null ? null : h.getDepartment().getDepartmentName(),
                                workload.getOrDefault(h.getId(), 0L)))
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public void assignDepartmentHead(Integer topicId, TopicAssignForm form, String secretaryEmail) {
        Account me = requireSecretary(secretaryEmail);
        Topic topic = topics.findByIdForUpdate(topicId).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_NOT_FOUND));
        if (topic.getStatus() != TopicStatus.ACTIVE) throw new ApplicationException(ErrorCode.TOPIC_NOT_ACTIVE);

        Account head = form.getDepartmentHeadId() == null ? null
                : accounts.findWithRoleAndDepartmentById(form.getDepartmentHeadId()).orElse(null);
        // R13: người nhận phải là Trưởng phòng; R20: Trưởng phòng phải thuộc một phòng ban; R23: đang ACTIVE
        if (head == null || head.getRole().getRoleName() != RoleName.DEPARTMENT_HEAD
                || head.getStatus() != AccountStatus.ACTIVE || head.getDepartment() == null)
            throw new ApplicationException(ErrorCode.TOPIC_HEAD_INVALID);

        OffsetDateTime now = now();
        OffsetDateTime deadline = form.getDeadline() == null ? null
                : form.getDeadline().atZone(ZoneId.systemDefault()).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS);
        if (deadline != null && !deadline.isAfter(now)) throw new ApplicationException(ErrorCode.ASSIGNMENT_DEADLINE_PAST);

        // UC-07: giao lại thì đóng Assignment cũ (REMOVED), không xóa dòng (NFR-02)
        Assignment current = assignments.findFirstByTopicAndAssignmentRoleAndStatusNot(
                topic, AssignmentRole.DEPARTMENT_HEAD, AssignmentStatus.REMOVED).orElse(null);
        if (current != null) {
            if (Objects.equals(current.getAccount().getId(), head.getId()))
                throw new ApplicationException(ErrorCode.TOPIC_HEAD_UNCHANGED);
            current.setStatus(AssignmentStatus.REMOVED);
            current.setCompletedAt(null);
            current.setRemovedAt(now);
            current.setRemovedBy(me);
            // Flush trước khi thêm dòng mới: Hibernate chạy INSERT trước UPDATE nên sẽ vi phạm uq_assignment_dept_head
            assignments.saveAndFlush(current);
        }

        Assignment assignment = new Assignment();
        assignment.setTopic(topic);
        assignment.setAccount(head);
        assignment.setAssignedBy(me);
        assignment.setAssignmentRole(AssignmentRole.DEPARTMENT_HEAD);
        assignment.setDeadline(deadline);
        assignment.setRequirements(trimToNull(form.getRequirements()));
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(now);
        assignments.save(assignment);
    }

    private List<EditorialTopicRowDTO> buildRows(boolean assigned) {
        List<Topic> active = topics.findByStatusOrderByCreatedAtDesc(TopicStatus.ACTIVE);
        if (active.isEmpty()) return List.of();
        Map<Integer, List<Assignment>> teamByTopic = assignments.findByTopicInAndStatusNot(active, AssignmentStatus.REMOVED).stream()
                .collect(Collectors.groupingBy(a -> a.getTopic().getId()));
        Map<Integer, Long> articleCounts = articles.countByTopicIds(active.stream().map(Topic::getId).toList()).stream()
                .collect(Collectors.toMap(r -> (Integer) r[0], r -> (Long) r[1]));

        return active.stream()
                .map(t -> toRow(t, teamByTopic.getOrDefault(t.getId(), List.of()), articleCounts.getOrDefault(t.getId(), 0L)))
                .filter(row -> (row.getDepartmentHeadName() != null) == assigned)
                .toList();
    }

    private EditorialTopicRowDTO toRow(Topic topic, List<Assignment> team, long articleCount) {
        Assignment head = findRole(team, AssignmentRole.DEPARTMENT_HEAD);
        Assignment lead = findRole(team, AssignmentRole.LEAD);
        return EditorialTopicRowDTO.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .category(topic.getCategory())
                .priority(topic.getPriority().name())
                .priorityLabel(priorityLabel(topic.getPriority()))
                .expectedDate(topic.getExpectedDate())
                .creatorName(topic.getCreator().getFullName())
                .editorial(topic.getCreator().getRole().getRoleName() == RoleName.EDITORIAL_SECRETARY)
                .createdAt(topic.getCreatedAt())
                .departmentHeadName(head == null ? null : head.getAccount().getFullName())
                .departmentName(head == null || head.getAccount().getDepartment() == null ? null
                        : head.getAccount().getDepartment().getDepartmentName())
                .assignedAt(head == null ? null : head.getAssignedAt())
                .leadName(lead == null ? null : lead.getAccount().getFullName())
                .supportingCount(team.stream().filter(a -> a.getAssignmentRole() == AssignmentRole.SUPPORTING).count())
                .articleCount(articleCount)
                .build();
    }

    private static Assignment findRole(List<Assignment> team, AssignmentRole role) {
        return team.stream().filter(a -> a.getAssignmentRole() == role).findFirst().orElse(null);
    }

    private Account requireSecretary(String email) {
        Account account = email == null ? null : accounts.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (account == null || account.getStatus() != AccountStatus.ACTIVE
                || account.getRole().getRoleName() != RoleName.EDITORIAL_SECRETARY)
            throw new ApplicationException(ErrorCode.EDITORIAL_TOPIC_ACCESS_DENIED);
        return account;
    }

    private static String trimToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static String priorityLabel(Priority priority) {
        return switch (priority) {
            case LOW -> "Thấp";
            case NORMAL -> "Bình thường";
            case HIGH -> "Cao";
            case URGENT -> "Khẩn";
        };
    }
}
