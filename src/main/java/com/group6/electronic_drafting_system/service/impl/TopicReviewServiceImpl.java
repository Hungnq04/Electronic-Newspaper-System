package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Topic.TopicHistoryEntryDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicReviewItemDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicReviewWorkspaceDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.TopicFeedback;
import com.group6.electronic_drafting_system.entity.TopicReview;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.entity.enums.TopicFeedbackType;
import com.group6.electronic_drafting_system.entity.enums.TopicReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.Reporter.ReporterTaskMapper;
import com.group6.electronic_drafting_system.mapper.Topic.TopicMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.DepartmentRepository;
import com.group6.electronic_drafting_system.repository.TopicFeedbackRepository;
import com.group6.electronic_drafting_system.repository.TopicRepository;
import com.group6.electronic_drafting_system.repository.TopicReviewRepository;
import com.group6.electronic_drafting_system.service.NotificationService;
import com.group6.electronic_drafting_system.service.TopicReviewService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicReviewServiceImpl implements TopicReviewService {
    private static final int FEEDBACK_MAX = 4000;

    private final TopicRepository topics;
    private final TopicReviewRepository topicReviews;
    private final TopicFeedbackRepository topicFeedbacks;
    private final AssignmentRepository assignments;
    private final AccountRepository accounts;
    private final DepartmentRepository departments;
    private final NotificationService notificationService;
    private final TopicMapper mapper;

    public TopicReviewServiceImpl(TopicRepository topics, TopicReviewRepository topicReviews,
                                  TopicFeedbackRepository topicFeedbacks, AssignmentRepository assignments,
                                  AccountRepository accounts, DepartmentRepository departments,
                                  NotificationService notificationService, TopicMapper mapper) {
        this.topics = topics;
        this.topicReviews = topicReviews;
        this.topicFeedbacks = topicFeedbacks;
        this.assignments = assignments;
        this.accounts = accounts;
        this.departments = departments;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    // ===================== Trưởng ban =====================

    @Override
    @Transactional(readOnly = true) //2
    public TopicReviewWorkspaceDTO getHeadWorkspace(String email, Integer selectedTopicId) {
        Account head = requireRole(email, RoleName.DEPARTMENT_HEAD);
        Integer departmentId = head.getDepartment().getId();

        // Hàng chờ: DEPARTMENT_REVIEW, đúng ban mình, không đang chờ phóng viên sửa
        List<TopicReviewItemDTO> queue = topics
                .findByStatusAndDepartment_IdAndAwaitingRevisionFalseOrderByCreatedAtAscIdAsc(TopicStatus.DEPARTMENT_REVIEW, departmentId)
                .stream().map(this::toItem).toList();

        Topic selected = pickSelected(selectedTopicId, queue);
        // Trưởng ban chỉ được xem đề tài của ban mình, kể cả khi gõ thẳng URL
        if (selected != null && !isSameDepartment(selected, head)) {
            throw new ApplicationException(ErrorCode.TOPIC_VIEW_FORBIDDEN);
        }
        boolean canDecide = selected != null && isWaitingForHead(selected);
        return new TopicReviewWorkspaceDTO(head.getDepartment().getDepartmentName(), queue,
                selected == null ? null : toItem(selected), historyOf(selected), canDecide, List.of(), null,
                head.getFullName(), "Trưởng ban · " + head.getDepartment().getDepartmentName(),
                ReporterTaskMapper.initials(head.getFullName()));
    }

    @Override
    @Transactional
    public void headApprove(String email, Integer topicId) {
        Account head = requireRole(email, RoleName.DEPARTMENT_HEAD);
        Topic topic = lockForHead(topicId, head);

        // Ghi kết quả duyệt cấp ban (R07: reviewer là Trưởng ban; R08: cùng ban với đề tài)
        TopicReview review = new TopicReview();
        review.setTopic(topic);
        review.setReviewer(head);
        review.setReviewStage(ReviewStage.DEPARTMENT);
        review.setDecision(TopicReviewDecision.APPROVED);
        topicReviews.save(review);

        // Đổi trạng thái: DEPARTMENT_REVIEW -> SECRETARY_REVIEW, nhớ Trưởng ban đã duyệt
        topic.setReviewedByHead(head);
        topic.setStatus(TopicStatus.SECRETARY_REVIEW);
    }

    @Override
    @Transactional //yeucausua
    public void headRequestChanges(String email, Integer topicId, String feedback) {
        Account head = requireRole(email, RoleName.DEPARTMENT_HEAD);
        String content = requireText(feedback, ErrorCode.TOPIC_FEEDBACK_REQUIRED);
        Topic topic = lockForHead(topicId, head);

        // Yêu cầu sửa KHÔNG ghi topic_review, chỉ ghi một vòng topic_feedback (được nhiều vòng)
        TopicFeedback changeRequest = new TopicFeedback();
        changeRequest.setTopic(topic);
        changeRequest.setAuthor(head);
        changeRequest.setType(TopicFeedbackType.CHANGE_REQUEST);
        changeRequest.setContent(content);
        topicFeedbacks.save(changeRequest);

        // Status giữ nguyên DEPARTMENT_REVIEW, chỉ bật cờ chờ phóng viên sửa
        topic.setAwaitingRevision(true);
        notificationService.notify(topic.getCreator(), topic,
                "Trưởng ban yêu cầu sửa đề tài \"" + topic.getTitle() + "\". Bấm để xem góp ý.");
    }

    // ===================== Thư ký tòa soạn =====================

    @Override
    @Transactional(readOnly = true)
    public TopicReviewWorkspaceDTO getSecretaryWorkspace(String email, Integer departmentFilter, Integer selectedTopicId) {
        Account secretary = requireRole(email, RoleName.EDITORIAL_SECRETARY);

        // Hàng chờ: SECRETARY_REVIEW của mọi ban, có thể lọc theo ban
        List<TopicReviewItemDTO> queue = topics.findByStatusOrderByCreatedAtAscIdAsc(TopicStatus.SECRETARY_REVIEW).stream()
                .filter(t -> departmentFilter == null
                        || (t.getDepartment() != null && departmentFilter.equals(t.getDepartment().getId())))
                .map(this::toItem).toList();

        Topic selected = pickSelected(selectedTopicId, queue);
        // Đề tài chưa qua Trưởng ban thì Thư ký không được xem ở màn này (NFR-03)
        if (selected != null && selected.getStatus() == TopicStatus.DEPARTMENT_REVIEW) {
            throw new ApplicationException(ErrorCode.TOPIC_VIEW_FORBIDDEN);
        }
        boolean canDecide = selected != null && selected.getStatus() == TopicStatus.SECRETARY_REVIEW;
        return new TopicReviewWorkspaceDTO("Tất cả các ban", queue, selected == null ? null : toItem(selected),
                historyOf(selected), canDecide,
                departments.findAll(Sort.by("departmentName")).stream().map(mapper::toOption).toList(), departmentFilter,
                secretary.getFullName(), "Thư ký tòa soạn", ReporterTaskMapper.initials(secretary.getFullName()));
    }

    @Override
    @Transactional
    public void secretaryApprove(String email, Integer topicId) {
        Account secretary = requireRole(email, RoleName.EDITORIAL_SECRETARY);
        Topic topic = lockForSecretary(topicId, secretary);

        // Trưởng ban đã duyệt: lấy từ topic.reviewed_by_head_id; dữ liệu cũ chưa có thì lấy từ topic_review cấp ban
        Account head = topic.getReviewedByHead();
        if (head == null) {
            head = topicReviews.findByTopic_IdAndReviewStage(topic.getId(), ReviewStage.DEPARTMENT)
                    .filter(r -> r.getDecision() == TopicReviewDecision.APPROVED)
                    .map(TopicReview::getReviewer).orElse(null);
            topic.setReviewedByHead(head);
        }
        Account reporter = topic.getCreator();
        // Người nhận assignment phải còn hoạt động và đúng vai trò (R13, R23)
        if (!isActiveWithRole(head, RoleName.DEPARTMENT_HEAD) || !isActiveWithRole(reporter, RoleName.REPORTER)) {
            throw new ApplicationException(ErrorCode.TOPIC_TEAM_UNAVAILABLE);
        }

        TopicReview review = new TopicReview();
        review.setTopic(topic);
        review.setReviewer(secretary);
        review.setReviewStage(ReviewStage.SECRETARY);
        review.setDecision(TopicReviewDecision.APPROVED);
        topicReviews.save(review);

        // Hệ thống tự tạo 2 assignment (CLAUDE.md 4.1-A): phóng viên đề xuất -> LEAD, Trưởng ban đã duyệt -> DEPARTMENT_HEAD.
        // Nhiệm vụ và deadline để trống, Trưởng ban bổ sung sau (OQ-06).
        assignments.save(newAssignment(topic, reporter, secretary, AssignmentRole.LEAD));
        assignments.save(newAssignment(topic, head, secretary, AssignmentRole.DEPARTMENT_HEAD));

        // Đổi trạng thái: SECRETARY_REVIEW -> ACTIVE
        topic.setStatus(TopicStatus.ACTIVE);
        notificationService.notify(reporter, topic,
                "Đề tài \"" + topic.getTitle() + "\" đã được duyệt. Bạn là phóng viên viết chính (Lead).");
    }

    @Override
    @Transactional
    public void secretaryReject(String email, Integer topicId, String reason) {
        Account secretary = requireRole(email, RoleName.EDITORIAL_SECRETARY);
        String content = requireText(reason, ErrorCode.TOPIC_REJECT_REASON_REQUIRED);
        Topic topic = lockForSecretary(topicId, secretary);

        // Từ chối: ghi topic_review cấp SECRETARY kèm lý do (bắt buộc, BR-12)
        TopicReview review = new TopicReview();
        review.setTopic(topic);
        review.setReviewer(secretary);
        review.setReviewStage(ReviewStage.SECRETARY);
        review.setDecision(TopicReviewDecision.REJECTED);
        review.setFeedback(content);
        topicReviews.save(review);

        // Đổi trạng thái: SECRETARY_REVIEW -> REJECTED (trạng thái cuối, không gửi lại được – OQ-08)
        topic.setStatus(TopicStatus.REJECTED);
        notificationService.notify(topic.getCreator(), topic,
                "Đề tài \"" + topic.getTitle() + "\" đã bị Thư ký tòa soạn từ chối. Bấm để xem lý do.");
    }

    // ===================== Hàm phụ =====================

    // Khóa dòng Topic rồi kiểm tra lại: đúng ban, còn DEPARTMENT_REVIEW và không đang chờ phóng viên sửa
    private Topic lockForHead(Integer topicId, Account head) {
        Topic topic = topics.findByIdForUpdate(topicId).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_NOT_FOUND));
        if (!isSameDepartment(topic, head)) throw new ApplicationException(ErrorCode.TOPIC_REVIEW_FORBIDDEN);
        // BR-34: không duyệt đề tài do chính mình đề xuất
        if (topic.getCreator().getId().equals(head.getId())) throw new ApplicationException(ErrorCode.TOPIC_REVIEW_FORBIDDEN);
        // Người khác đã xử lý trước (hoặc hai người bấm cùng lúc): người sau nhận lỗi
        if (!isWaitingForHead(topic)) throw new ApplicationException(ErrorCode.TOPIC_ALREADY_PROCESSED);
        return topic;
    }

    // Khóa dòng Topic rồi kiểm tra lại: còn SECRETARY_REVIEW
    private Topic lockForSecretary(Integer topicId, Account secretary) {
        Topic topic = topics.findByIdForUpdate(topicId).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_NOT_FOUND));
        // BR-34: không duyệt đề tài do chính mình đề xuất
        if (topic.getCreator().getId().equals(secretary.getId())) throw new ApplicationException(ErrorCode.TOPIC_REVIEW_FORBIDDEN);
        if (topic.getStatus() != TopicStatus.SECRETARY_REVIEW) throw new ApplicationException(ErrorCode.TOPIC_ALREADY_PROCESSED);
        return topic;
    }

    // Người đăng nhập phải đúng role và đang ACTIVE; Trưởng ban phải có ban
    private Account requireRole(String email, RoleName role) {
        Account me = email == null ? null : accounts.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (!isActiveWithRole(me, role) || (role == RoleName.DEPARTMENT_HEAD && me.getDepartment() == null)) {
            throw new ApplicationException(ErrorCode.TOPIC_REVIEW_FORBIDDEN);
        }
        return me;
    }

    // Đề tài đang mở chi tiết: theo URL, nếu không có thì lấy đề tài đầu hàng chờ
    private Topic pickSelected(Integer selectedTopicId, List<TopicReviewItemDTO> queue) {
        Integer id = selectedTopicId != null ? selectedTopicId : (queue.isEmpty() ? null : queue.get(0).id());
        if (id == null) return null;
        return topics.findWithPeopleById(id).orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_NOT_FOUND));
    }

    private TopicReviewItemDTO toItem(Topic topic) {
        int rounds = (int) topicFeedbacks.countByTopic_IdAndType(topic.getId(), TopicFeedbackType.CHANGE_REQUEST);
        return mapper.toReviewItem(topic, rounds);
    }

    private List<TopicHistoryEntryDTO> historyOf(Topic topic) {
        if (topic == null) return List.of();
        return mapper.toHistory(topicFeedbacks.findByTopic_IdOrderByCreatedAtAscIdAsc(topic.getId()),
                topicReviews.findByTopic_IdOrderByReviewedAtAscIdAsc(topic.getId()));
    }

    private static Assignment newAssignment(Topic topic, Account account, Account assignedBy, AssignmentRole role) {
        Assignment assignment = new Assignment();
        assignment.setTopic(topic);
        assignment.setAccount(account);
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignmentRole(role);
        return assignment;
    }

    private static boolean isWaitingForHead(Topic topic) {
        return topic.getStatus() == TopicStatus.DEPARTMENT_REVIEW && !topic.isAwaitingRevision();
    }

    private static boolean isSameDepartment(Topic topic, Account head) {
        return topic.getDepartment() != null && topic.getDepartment().getId().equals(head.getDepartment().getId());
    }

    private static boolean isActiveWithRole(Account account, RoleName role) {
        return account != null && account.getStatus() == AccountStatus.ACTIVE && account.getRole().getRoleName() == role;
    }

    // Góp ý / lý do bắt buộc, tối đa 4000 ký tự
    private static String requireText(String text, ErrorCode missing) {
        if (text == null || text.isBlank()) throw new ApplicationException(missing);
        String trimmed = text.trim();
        if (trimmed.length() > FEEDBACK_MAX) throw new ApplicationException(ErrorCode.TOPIC_FEEDBACK_TOO_LONG);
        return trimmed;
    }
}
