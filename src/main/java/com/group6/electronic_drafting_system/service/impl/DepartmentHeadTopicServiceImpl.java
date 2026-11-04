package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ChangeLeadFormDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DepartmentHeadTopicBoardDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DepartmentHeadTopicDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentFormDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentPageDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentState;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.TopicReview;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.DepartmentHeadTopicMapper;
import com.group6.electronic_drafting_system.mapper.Reporter.ReporterTaskMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.TopicReviewRepository;
import com.group6.electronic_drafting_system.service.DepartmentHeadTopicService;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentHeadTopicServiceImpl implements DepartmentHeadTopicService {
    // Đề tài cần xử lý lên trước (Chưa có Lead -> Chờ giao nhiệm vụ -> Đã giao), cùng mức thì mới duyệt lên trước
    private static final Comparator<DepartmentHeadTopicDTO> BY_STATE = Comparator
            .comparing(DepartmentHeadTopicDTO::state)
            .thenComparing(DepartmentHeadTopicDTO::approvedAt, Comparator.reverseOrder());

    private final AssignmentRepository assignments;
    private final TopicReviewRepository topicReviews;
    // Bài đang trong quy trình duyệt: Lead cũ còn bài ở các trạng thái này thì không cho đổi Lead
    private static final Set<ArticleStatus> IN_REVIEW = EnumSet.of(
            ArticleStatus.DEPARTMENT_REVIEW, ArticleStatus.DEPARTMENT_APPROVED, ArticleStatus.APPROVED, ArticleStatus.SCHEDULED);

    private final AccountRepository accounts;
    private final ArticleRepository articles;
    private final DepartmentHeadTopicMapper mapper;

    public DepartmentHeadTopicServiceImpl(AssignmentRepository assignments, TopicReviewRepository topicReviews,
                                          AccountRepository accounts, ArticleRepository articles, DepartmentHeadTopicMapper mapper) {
        this.assignments = assignments;
        this.topicReviews = topicReviews;
        this.accounts = accounts;
        this.articles = articles;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentHeadTopicBoardDTO getBoard(String email) {
        Account me = currentAccount(email);
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Kiểm tra quyền ở backend: chỉ lấy đề tài ACTIVE mà chính người đăng nhập đang là DEPARTMENT_HEAD
        // (assignment chưa REMOVED). Trưởng phòng khác không thấy đề tài không phải của mình.
        List<Topic> topics = assignments.findByAccount_IdAndAssignmentRoleAndStatusNotAndTopic_Status(
                        me.getId(), AssignmentRole.DEPARTMENT_HEAD, AssignmentStatus.REMOVED, TopicStatus.ACTIVE)
                .stream().map(Assignment::getTopic).toList();
        if (topics.isEmpty()) return board(me, List.of());

        // Nạp một lần thành viên + kết quả duyệt cấp Thư ký của tất cả đề tài, tránh truy vấn theo từng dòng
        List<Integer> topicIds = topics.stream().map(Topic::getId).toList();
        Map<Integer, List<Assignment>> teamByTopic = assignments.findByTopic_IdInAndStatusNot(topicIds, AssignmentStatus.REMOVED)
                .stream().collect(Collectors.groupingBy(a -> a.getTopic().getId()));
        Map<Integer, TopicReview> secretaryReviewByTopic = topicReviews.findByTopic_IdInAndReviewStage(topicIds, ReviewStage.SECRETARY)
                .stream().collect(Collectors.toMap(r -> r.getTopic().getId(), Function.identity()));

        List<DepartmentHeadTopicDTO> rows = topics.stream()
                .map(t -> mapper.toTopicDTO(t, teamByTopic.getOrDefault(t.getId(), List.of()), secretaryReviewByTopic.get(t.getId()), now))
                .sorted(BY_STATE)
                .toList();
        return board(me, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public TopicAssignmentPageDTO getAssignmentPage(Integer topicId, String email) {
        Account me = currentAccount(email);
        Topic topic = myTopic(topicId, me);
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // Lấy danh sách assignment chưa REMOVED của đề tài, để hiển thị Trưởng phóng viên + Supporting
        List<Assignment> team = assignments.findByTopicAndStatusNot(topic, AssignmentStatus.REMOVED);
        Assignment lead = findLead(team);
        TopicReview secretaryReview = topicReviews.findByTopic_IdInAndReviewStage(List.of(topicId), ReviewStage.SECRETARY)
                .stream().findFirst().orElse(null);
        DepartmentHeadTopicDTO summary = mapper.toTopicDTO(topic, team, secretaryReview, now);

        List<TopicAssignmentPageDTO.Supporter> supporters = team.stream()
                .filter(a -> a.getAssignmentRole() == AssignmentRole.SUPPORTING)
                .map(mapper::toSupporter)
                .toList();

        // Ứng viên: phóng viên ACTIVE chưa có assignment hiệu lực trên đề tài (BR-24: một người một vai trò / đề tài).
        // Phóng viên cùng phòng với Trưởng phòng xếp lên đầu, sau đó theo tên.
        Set<Integer> inTopic = team.stream().map(a -> a.getAccount().getId()).collect(Collectors.toSet());
        Integer myDepartmentId = me.getDepartment() == null ? null : me.getDepartment().getId();
        List<TopicAssignmentPageDTO.Person> candidates = accounts.findByRole_RoleNameAndStatusOrderByFullNameAsc(RoleName.REPORTER, AccountStatus.ACTIVE)
                .stream()
                .filter(a -> !inTopic.contains(a.getId()))
                .map(a -> mapper.toPerson(a, myDepartmentId))
                .sorted(Comparator.comparing((TopicAssignmentPageDTO.Person p) -> !p.sameDepartment()))
                .toList();

        // Người có thể nhận Lead mới: ứng viên chưa tham gia + Supporting đang làm (ASSIGNED).
        // Supporting đã COMPLETED không được chọn vì dòng của họ không được gỡ.
        List<TopicAssignmentPageDTO.LeadOption> leadOptions = new ArrayList<>();
        team.stream()
                .filter(a -> a.getAssignmentRole() == AssignmentRole.SUPPORTING && a.getStatus() == AssignmentStatus.ASSIGNED)
                .forEach(a -> leadOptions.add(mapper.toLeadOption(a.getAccount(), true)));
        accounts.findByRole_RoleNameAndStatusOrderByFullNameAsc(RoleName.REPORTER, AccountStatus.ACTIVE).stream()
                .filter(a -> !inTopic.contains(a.getId()))
                .forEach(a -> leadOptions.add(mapper.toLeadOption(a, false)));

        long blocking = lead == null ? 0 : articles.findByTopic_IdAndLead_Id(topicId, lead.getAccount().getId()).stream()
                .filter(a -> IN_REVIEW.contains(a.getStatus()))
                .count();
        List<TopicAssignmentPageDTO.HistoryItem> history = assignments.findByTopic_IdAndStatusOrderByRemovedAtDesc(topicId, AssignmentStatus.REMOVED)
                .stream().map(mapper::toHistoryItem).toList();

        String departmentName = me.getDepartment() == null ? null : me.getDepartment().getDepartmentName();
        return new TopicAssignmentPageDTO(me.getFullName(), departmentName, ReporterTaskMapper.initials(me.getFullName()),
                summary, topic.getRationale(), topic.getExpectedDate(),
                lead == null ? null : mapper.toPerson(lead.getAccount(), myDepartmentId),
                lead == null ? null : lead.getAssignedAt(), lead == null ? null : lead.getDeadline(), lead == null ? null : lead.getRequirements(),
                supporters, candidates, summary.state() != TopicAssignmentState.ASSIGNED,
                leadOptions, blocking, history);
    }

    // Department Head bấm "Xác nhận giao việc" (FL-03).
    // Mọi thay đổi nằm trong MỘT transaction: lỗi ở bất kỳ người nào thì không lưu gì cả.
    @Override
    @Transactional
    public void confirmAssignment(Integer topicId, TopicAssignmentFormDTO form, String email) {
        Account me = currentAccount(email);
        Topic topic = myTopic(topicId, me);   // Kiểm tra quyền: đúng Trưởng phòng của đề tài, đề tài ACTIVE
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        List<Assignment> team = assignments.findByTopicAndStatusNot(topic, AssignmentStatus.REMOVED);
        Assignment lead = findLead(team);
        // Đã giao xong thì không giao lại ở đây: dùng phần quản lý nhóm (thêm / gỡ Supporting, đổi Lead)
        if (DepartmentHeadTopicMapper.stateOf(lead) == TopicAssignmentState.ASSIGNED) {
            throw new ApplicationException(ErrorCode.ASSIGNMENT_ALREADY_DONE);
        }

        // Những người đang có vai trò trên đề tài (kể cả Trưởng phòng): không được nhận thêm vai trò nữa (BR-24, BR-38)
        Set<Integer> taken = team.stream().map(a -> a.getAccount().getId()).collect(Collectors.toCollection(HashSet::new));

        // 1. Lead: đúng một người
        if (lead == null) {
            // Đề tài Thư ký giao xuống: Trưởng phòng chọn Lead
            if (form.getLeadAccountId() == null) throw new ApplicationException(ErrorCode.ASSIGNMENT_LEAD_REQUIRED);
            if (taken.contains(form.getLeadAccountId())) throw new ApplicationException(ErrorCode.ASSIGNMENT_DUPLICATE_MEMBER);
            lead = newAssignment(topic, activeReporter(form.getLeadAccountId()), me, AssignmentRole.LEAD);
        } else if (form.getLeadAccountId() != null && !form.getLeadAccountId().equals(lead.getAccount().getId())) {
            // Đề tài đã có Lead (người đề xuất): chỉ bổ sung nhiệm vụ + deadline, không đổi người
            throw new ApplicationException(ErrorCode.ASSIGNMENT_LEAD_ALREADY_SET);
        }
        lead.setDeadline(parseDeadline(form.getLeadDeadline(), now));
        lead.setRequirements(requiredText(form.getLeadRequirements()));
        assignments.save(lead);
        taken.add(lead.getAccount().getId());

        // 2. Supporting (không bắt buộc)
        saveSupporters(topic, form, me, taken, now);
    }

    // Thêm Supporting vào đề tài đã giao xong (phần quản lý nhóm)
    @Override
    @Transactional
    public void addSupporters(Integer topicId, TopicAssignmentFormDTO form, String email) {
        Account me = currentAccount(email);
        Topic topic = myTopic(topicId, me);   // Kiểm tra quyền: đúng Trưởng phòng của đề tài
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        List<Assignment> team = assignments.findByTopicAndStatusNot(topic, AssignmentStatus.REMOVED);
        if (DepartmentHeadTopicMapper.stateOf(findLead(team)) != TopicAssignmentState.ASSIGNED) {
            throw new ApplicationException(ErrorCode.TEAM_NOT_READY);
        }
        if (form.getSupporterIds().isEmpty()) throw new ApplicationException(ErrorCode.SUPPORTERS_REQUIRED);

        Set<Integer> taken = team.stream().map(a -> a.getAccount().getId()).collect(Collectors.toCollection(HashSet::new));
        saveSupporters(topic, form, me, taken, now);
    }

    // Gỡ Supporting (EC-02): KHÔNG xóa dòng, chỉ đổi sang REMOVED. Source họ đã thêm được giữ nguyên.
    @Override
    @Transactional
    public void removeSupporter(Integer topicId, Integer assignmentId, String email) {
        Account me = currentAccount(email);
        Topic topic = myTopic(topicId, me);   // Kiểm tra quyền: đúng Trưởng phòng của đề tài
        Assignment supporter = assignments.findWithDetailsById(assignmentId)
                .filter(a -> a.getTopic().getId().equals(topic.getId()))
                .filter(a -> a.getAssignmentRole() == AssignmentRole.SUPPORTING && a.getStatus() != AssignmentStatus.REMOVED)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        // Supporting đã xong phần việc thì không gỡ
        if (supporter.getStatus() == AssignmentStatus.COMPLETED) throw new ApplicationException(ErrorCode.ASSIGNMENT_REMOVE_COMPLETED);

        markRemoved(supporter, me);
        assignments.save(supporter);
    }

    // Đổi Lead (EC-01). Trong MỘT transaction:
    //   1) đóng dòng Lead cũ (REMOVED); nếu Lead mới đang là Supporting thì đóng luôn dòng Supporting đó;
    //   2) tạo dòng LEAD mới với nhiệm vụ + deadline;
    //   3) chuyển bài DRAFT / REVISION_REQUIRED của Lead cũ sang Lead mới (article.lead_id).
    // Lead cũ còn bài đang duyệt thì chặn, không đổi gì.
    @Override
    @Transactional
    public void changeLead(Integer topicId, ChangeLeadFormDTO form, String email) {
        Account me = currentAccount(email);
        Topic topic = myTopic(topicId, me);   // Kiểm tra quyền: đúng Trưởng phòng của đề tài
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        List<Assignment> team = assignments.findByTopicAndStatusNot(topic, AssignmentStatus.REMOVED);
        Assignment oldLead = findLead(team);
        if (DepartmentHeadTopicMapper.stateOf(oldLead) != TopicAssignmentState.ASSIGNED) {
            throw new ApplicationException(ErrorCode.TEAM_NOT_READY);
        }

        // Kiểm tra hết trước khi thay đổi bất cứ thứ gì
        Integer newLeadId = form.getNewLeadAccountId();
        if (newLeadId == null) throw new ApplicationException(ErrorCode.ASSIGNMENT_LEAD_REQUIRED);
        if (newLeadId.equals(oldLead.getAccount().getId())) throw new ApplicationException(ErrorCode.LEAD_CHANGE_SAME_PERSON);
        Account newLeadAccount = activeReporter(newLeadId);
        OffsetDateTime deadline = parseDeadline(form.getDeadline(), now);
        String requirements = requiredText(form.getRequirements());

        // Lead mới đang có vai trò trong đề tài? Chỉ chấp nhận nếu là Supporting đang làm (không phải COMPLETED)
        Assignment currentRole = team.stream().filter(a -> a.getAccount().getId().equals(newLeadId)).findFirst().orElse(null);
        if (currentRole != null) {
            if (currentRole.getAssignmentRole() != AssignmentRole.SUPPORTING) throw new ApplicationException(ErrorCode.ASSIGNMENT_DUPLICATE_MEMBER);
            if (currentRole.getStatus() == AssignmentStatus.COMPLETED) throw new ApplicationException(ErrorCode.ASSIGNMENT_REMOVE_COMPLETED);
        }

        List<Article> oldLeadArticles = articles.findByTopic_IdAndLead_Id(topic.getId(), oldLead.getAccount().getId());
        if (oldLeadArticles.stream().anyMatch(a -> IN_REVIEW.contains(a.getStatus()))) {
            throw new ApplicationException(ErrorCode.LEAD_CHANGE_BLOCKED_BY_REVIEW);
        }

        // 1. Đóng dòng cũ. saveAndFlush = ghi xuống DB NGAY: nếu không, Hibernate sẽ INSERT dòng Lead mới trước
        //    khi UPDATE dòng cũ và unique index "1 Lead / 1 người trên Topic" sẽ báo lỗi.
        markRemoved(oldLead, me);
        assignments.saveAndFlush(oldLead);
        if (currentRole != null) {
            markRemoved(currentRole, me);
            assignments.saveAndFlush(currentRole);
        }

        // 2. Dòng LEAD mới
        Assignment newLead = newAssignment(topic, newLeadAccount, me, AssignmentRole.LEAD);
        newLead.setDeadline(deadline);
        newLead.setRequirements(requirements);
        assignments.save(newLead);

        // 3. Chuyển bài Lead cũ đang giữ sang Lead mới; bài PUBLISHED giữ nguyên tên Lead cũ
        for (Article article : oldLeadArticles) {
            if (article.getStatus() == ArticleStatus.DRAFT || article.getStatus() == ArticleStatus.REVISION_REQUIRED) {
                article.setLead(newLeadAccount);
                articles.save(article);
            }
        }
    }

    // Tạo các dòng Supporting từ form: mỗi người một nhiệm vụ + deadline riêng.
    // taken: account_id đang có vai trò trên đề tài; người trong taken không được thêm (BR-24: một người một vai trò).
    private void saveSupporters(Topic topic, TopicAssignmentFormDTO form, Account me, Set<Integer> taken, OffsetDateTime now) {
        for (Integer accountId : form.getSupporterIds().stream().distinct().toList()) {
            if (taken.contains(accountId)) throw new ApplicationException(ErrorCode.ASSIGNMENT_DUPLICATE_MEMBER);
            Assignment supporter = newAssignment(topic, activeReporter(accountId), me, AssignmentRole.SUPPORTING);
            supporter.setDeadline(parseDeadline(form.getSupporterDeadlines().get(accountId), now));
            supporter.setRequirements(requiredText(form.getSupporterRequirements().get(accountId)));
            assignments.save(supporter);
            taken.add(accountId);
        }
    }

    // Đóng một assignment: không xóa dòng, ghi lại ai gỡ và lúc nào (giữ lịch sử, NFR-02)
    private static void markRemoved(Assignment assignment, Account by) {
        assignment.setStatus(AssignmentStatus.REMOVED);
        assignment.setRemovedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        assignment.setRemovedBy(by);
    }

    // Đề tài ACTIVE mà người đăng nhập đang là DEPARTMENT_HEAD (assignment chưa REMOVED); không thì 403
    private Topic myTopic(Integer topicId, Account me) {
        return assignments.findFirstByTopic_IdAndAccount_IdAndAssignmentRoleAndStatusNot(
                        topicId, me.getId(), AssignmentRole.DEPARTMENT_HEAD, AssignmentStatus.REMOVED)
                .map(Assignment::getTopic)
                .filter(t -> t.getStatus() == TopicStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_NOT_MANAGED));
    }

    private static Assignment findLead(List<Assignment> team) {
        return team.stream().filter(a -> a.getAssignmentRole() == AssignmentRole.LEAD).findFirst().orElse(null);
    }

    // Người được giao phải là Reporter đang ACTIVE (rule R23: tài khoản không ACTIVE không giữ assignment)
    private Account activeReporter(Integer accountId) {
        return accounts.findById(accountId)
                .filter(a -> a.getRole().getRoleName() == RoleName.REPORTER && a.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ASSIGNMENT_INVALID_REPORTER));
    }

    private static Assignment newAssignment(Topic topic, Account account, Account assignedBy, AssignmentRole role) {
        Assignment assignment = new Assignment();
        assignment.setTopic(topic);
        assignment.setAccount(account);
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignmentRole(role);
        return assignment;   // status mặc định ASSIGNED
    }

    // Deadline bắt buộc và phải ở tương lai
    private static OffsetDateTime parseDeadline(String value, OffsetDateTime now) {
        if (value == null || value.isBlank()) throw new ApplicationException(ErrorCode.ASSIGNMENT_DEADLINE_REQUIRED);
        OffsetDateTime deadline;
        try {
            deadline = DepartmentHeadTopicMapper.fromInputValue(value);
        } catch (DateTimeParseException ex) {
            throw new ApplicationException(ErrorCode.ASSIGNMENT_DEADLINE_INVALID);
        }
        if (!deadline.isAfter(now)) throw new ApplicationException(ErrorCode.ASSIGNMENT_DEADLINE_INVALID);
        return deadline;
    }

    private static String requiredText(String value) {
        if (value == null || value.isBlank()) throw new ApplicationException(ErrorCode.ASSIGNMENT_REQUIREMENTS_REQUIRED);
        return value.trim();
    }

    private static DepartmentHeadTopicBoardDTO board(Account me, List<DepartmentHeadTopicDTO> topics) {
        Map<TopicAssignmentState, Long> stateCounts = topics.stream()
                .collect(Collectors.groupingBy(DepartmentHeadTopicDTO::state, Collectors.counting()));
        List<String> categories = topics.stream().map(DepartmentHeadTopicDTO::category).distinct().sorted().toList();
        String departmentName = me.getDepartment() == null ? null : me.getDepartment().getDepartmentName();
        return new DepartmentHeadTopicBoardDTO(me.getFullName(), departmentName, ReporterTaskMapper.initials(me.getFullName()),
                topics, stateCounts, categories);
    }

    private Account currentAccount(String email) {
        if (email == null) throw new ApplicationException(ErrorCode.CURRENT_ACCOUNT_NOT_FOUND);
        return accounts.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new ApplicationException(ErrorCode.CURRENT_ACCOUNT_NOT_FOUND));
    }
}
