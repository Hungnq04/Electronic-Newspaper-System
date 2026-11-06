package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Topic.DepartmentOptionDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicFeedbackViewDTO;
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
import com.group6.electronic_drafting_system.repository.TopicFeedbackRepository;
import com.group6.electronic_drafting_system.repository.TopicRepository;
import com.group6.electronic_drafting_system.repository.TopicReviewRepository;
import com.group6.electronic_drafting_system.service.NotificationService;
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
    private final TopicFeedbackRepository topicFeedbacks;
    private final TopicReviewRepository topicReviews;
    private final NotificationService notificationService;
    private final TopicMapper mapper;

    public TopicServiceImpl(TopicRepository topics, DepartmentRepository departments, AccountRepository accounts,
                            TopicFeedbackRepository topicFeedbacks, TopicReviewRepository topicReviews,
                            NotificationService notificationService, TopicMapper mapper) {
        this.topics = topics;
        this.departments = departments;
        this.accounts = accounts;
        this.topicFeedbacks = topicFeedbacks;
        this.topicReviews = topicReviews;
        this.notificationService = notificationService;
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
        // Chuông thông báo chỉ dành cho phóng viên
        boolean reporter = me.getRole().getRoleName() == RoleName.REPORTER;
        return new TopicProposalListDTO(me.getFullName(), roleLabel(me), ReporterTaskMapper.initials(me.getFullName()), tabs, items,
                reporter ? notificationService.countUnread(me.getId()) : 0,
                reporter ? notificationService.getRecent(me.getId()) : List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentOptionDTO getMyDepartment(String email) {
        return currentAccount(email).map(Account::getDepartment).map(mapper::toOption).orElse(null);
    }

    @Override
    @Transactional
    public Integer createProposal(String email, TopicCreateRequestDTO request) {
        // Kiểm tra lại ở server dù SecurityConfig đã chặn /reporter/** cho người không phải REPORTER
        Account me = requireActiveReporter(email, ErrorCode.TOPIC_CREATE_FORBIDDEN);
        validate(request);

        // Phóng viên chỉ đề xuất đề tài cho ban của chính mình (không chọn ban khác)
        if (me.getDepartment() == null) throw new ApplicationException(ErrorCode.TOPIC_REPORTER_NO_DEPARTMENT);
        Integer myDepartmentId = me.getDepartment().getId();
        // Ban phải có Trưởng ban đang hoạt động, nếu không đề tài sẽ không có ai duyệt
        Department department = selectableDepartments().stream()
                .filter(d -> d.getId().equals(myDepartmentId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_DEPARTMENT_INVALID));

        return topics.save(mapper.toNewTopic(request, department, me)).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public TopicFeedbackViewDTO getFeedbackView(String email, Integer topicId) {
        Account me = currentAccount(email).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_ACCESS_DENIED));
        Topic topic = topics.findWithPeopleById(topicId).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_NOT_FOUND));
        // Chỉ chủ đề tài được xem góp ý / lý do, kể cả khi gõ thẳng URL
        if (!topic.getCreator().getId().equals(me.getId())) throw new ApplicationException(ErrorCode.TOPIC_VIEW_FORBIDDEN);

        return new TopicFeedbackViewDTO(mapper.toListItem(topic),
                mapper.toHistory(topicFeedbacks.findByTopic_IdOrderByCreatedAtAscIdAsc(topicId),
                        topicReviews.findByTopic_IdOrderByReviewedAtAscIdAsc(topicId)),
                isWaitingForReporter(topic));
    }

    @Override
    @Transactional(readOnly = true)
    public TopicCreateRequestDTO getEditForm(String email, Integer topicId) {
        Account me = requireActiveReporter(email, ErrorCode.TOPIC_EDIT_FORBIDDEN);
        Topic topic = topics.findById(topicId).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_NOT_FOUND));
        checkCanEdit(topic, me);

        TopicCreateRequestDTO form = new TopicCreateRequestDTO();
        form.setTitle(topic.getTitle());
        form.setSummary(topic.getDescription());
        return form;
    }

    @Override
    @Transactional
    public void resubmit(String email, Integer topicId, TopicCreateRequestDTO request) {
        Account me = requireActiveReporter(email, ErrorCode.TOPIC_EDIT_FORBIDDEN);
        validate(request);
        // Khóa dòng Topic rồi kiểm tra lại trạng thái trong DB (tránh gửi lại 2 lần cùng lúc)
        Topic topic = topics.findByIdForUpdate(topicId).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_NOT_FOUND));
        checkCanEdit(topic, me);

        // Chỉ sửa tiêu đề và tóm tắt; ban phụ trách giữ nguyên
        topic.setTitle(request.getTitle().trim());
        topic.setDescription(request.getSummary().trim());
        // Gửi lại: tắt cờ chờ sửa, đề tài quay về hàng chờ của Trưởng ban (status vẫn DEPARTMENT_REVIEW)
        topic.setAwaitingRevision(false);
    }

    // Chỉ chủ đề tài, và chỉ khi Trưởng ban đang yêu cầu sửa
    private static void checkCanEdit(Topic topic, Account me) {
        if (!topic.getCreator().getId().equals(me.getId()) || !isWaitingForReporter(topic)) {
            throw new ApplicationException(ErrorCode.TOPIC_EDIT_FORBIDDEN);
        }
    }

    private static boolean isWaitingForReporter(Topic topic) {
        return topic.getStatus() == TopicStatus.DEPARTMENT_REVIEW && topic.isAwaitingRevision();
    }

    private Account requireActiveReporter(String email, ErrorCode error) {
        return currentAccount(email)
                .filter(a -> a.getRole().getRoleName() == RoleName.REPORTER && a.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(error));
    }

    private static void validate(TopicCreateRequestDTO request) {
        if (request == null || isBlank(request.getTitle()) || isBlank(request.getSummary())
                || request.getTitle().trim().length() > TopicCreateRequestDTO.TITLE_MAX
                || request.getSummary().trim().length() > TopicCreateRequestDTO.SUMMARY_MAX) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED);
        }
    }

    // Phạm vi Topic theo vai trò. Màn danh sách hiện chỉ mở cho REPORTER; hai nhánh còn lại để sẵn.
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
