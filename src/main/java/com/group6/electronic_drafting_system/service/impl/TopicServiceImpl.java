package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Topic.DepartmentOptionDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicListItemDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicProposalListDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicProposalListDTO.StatusTab;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.Reporter.ReporterTaskMapper;
import com.group6.electronic_drafting_system.mapper.Topic.TopicMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.DepartmentRepository;
import com.group6.electronic_drafting_system.repository.TopicRepository;
import com.group6.electronic_drafting_system.service.TopicService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicServiceImpl implements TopicService {
    private final TopicRepository topics;
    private final DepartmentRepository departments;
    private final AccountRepository accounts;
    private final TopicMapper mapper;

    public TopicServiceImpl(TopicRepository topics, DepartmentRepository departments, AccountRepository accounts, TopicMapper mapper) {
        this.topics = topics;
        this.departments = departments;
        this.accounts = accounts;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public TopicProposalListDTO getProposalList(String email, TopicStatus statusFilter) {
        Account me = currentAccount(email).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_ACCESS_DENIED));
        List<Topic> visible = visibleTopics(me);

        Map<TopicStatus, Long> counts = visible.stream().collect(Collectors.groupingBy(Topic::getStatus, Collectors.counting()));
        List<StatusTab> tabs = new ArrayList<>();
        tabs.add(new StatusTab(null, "Tất cả", visible.size(), statusFilter == null));
        for (TopicStatus status : TopicStatus.values()) {
            tabs.add(new StatusTab(status, TopicListItemDTO.labelOf(status), counts.getOrDefault(status, 0L), status == statusFilter));
        }

        List<TopicListItemDTO> items = visible.stream()
                .filter(t -> statusFilter == null || t.getStatus() == statusFilter)
                .map(mapper::toListItem)
                .toList();
        return new TopicProposalListDTO(me.getFullName(), roleLabel(me), ReporterTaskMapper.initials(me.getFullName()), tabs, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentOptionDTO> getDepartmentOptions() {
        return selectableDepartments().stream().map(mapper::toOption).toList();
    }

    @Override
    @Transactional
    public Integer createProposal(String email, TopicCreateRequestDTO request) {
        // Kiểm tra lại ở server dù SecurityConfig đã chặn /reporter/** cho người không phải REPORTER
        Account me = currentAccount(email)
                .filter(a -> a.getRole().getRoleName() == RoleName.REPORTER && a.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_CREATE_FORBIDDEN));
        if (request == null || isBlank(request.getTitle()) || isBlank(request.getSummary()) || request.getDepartmentId() == null
                || request.getTitle().trim().length() > TopicCreateRequestDTO.TITLE_MAX
                || request.getSummary().trim().length() > TopicCreateRequestDTO.SUMMARY_MAX) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED);
        }
        Department department = selectableDepartments().stream()
                .filter(d -> d.getId().equals(request.getDepartmentId()))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_DEPARTMENT_INVALID));

        return topics.save(mapper.toNewTopic(request, department, me)).getId();
    }

    // Phạm vi Topic theo vai trò. Iteration 1 chỉ mở màn cho REPORTER; hai nhánh còn lại để sẵn cho màn duyệt.
    private List<Topic> visibleTopics(Account me) {
        return switch (me.getRole().getRoleName()) {
            case REPORTER -> topics.findByCreator_IdOrderByCreatedAtDescIdDesc(me.getId());
            case DEPARTMENT_HEAD -> me.getDepartment() == null ? List.of()
                    : topics.findByDepartment_IdOrderByCreatedAtDescIdDesc(me.getDepartment().getId());
            case EDITORIAL_SECRETARY -> topics.findByStatusOrderByCreatedAtDescIdDesc(TopicStatus.SECRETARY_REVIEW);
            case ADMIN -> List.of();
        };
    }

    // Chỉ ban có Trưởng ban đang hoạt động (Topic được gửi tới Trưởng ban của ban đó)
    private List<Department> selectableDepartments() {
        return departments.findHavingAccountWithRole(RoleName.DEPARTMENT_HEAD, AccountStatus.ACTIVE);
    }

    private Optional<Account> currentAccount(String email) {
        if (isBlank(email)) return Optional.empty();
        return accounts.findByEmailIgnoreCase(email.trim());
    }

    private static String roleLabel(Account me) {
        String role = switch (me.getRole().getRoleName()) {
            case REPORTER -> "Phóng viên";
            case DEPARTMENT_HEAD -> "Trưởng ban";
            case EDITORIAL_SECRETARY -> "Thư ký tòa soạn";
            case ADMIN -> "Quản trị viên";
        };
        return me.getDepartment() == null ? role : role + " · " + me.getDepartment().getDepartmentName();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
