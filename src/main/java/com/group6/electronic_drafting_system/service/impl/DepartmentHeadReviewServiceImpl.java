package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewCommentDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewWorkspaceDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Comment;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.ReviewWorkspaceMapper;
import com.group6.electronic_drafting_system.mapper.Reporter.ReporterTaskMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleVersionRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.CommentRepository;
import com.group6.electronic_drafting_system.service.DepartmentHeadReviewService;
import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.ReviewKind;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.repository.ArticleReviewRepository;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartmentHeadReviewServiceImpl implements DepartmentHeadReviewService {
    private final AccountRepository accounts;
    private final ArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final AssignmentRepository assignments;
    private final ArticleReviewRepository articleReviews;
    private final ReviewWorkspaceMapper mapper;
    private final CommentRepository comments;

    public DepartmentHeadReviewServiceImpl(AccountRepository accounts, ArticleRepository articles,
            ArticleVersionRepository versions, AssignmentRepository assignments,
            ReviewWorkspaceMapper mapper, ArticleReviewRepository articleReviews, CommentRepository comments) {
        this.accounts = accounts;
        this.articles = articles;
        this.versions = versions;
        this.assignments = assignments;
        this.articleReviews = articleReviews;
        this.mapper = mapper;
        this.comments = comments;
    }

    @Override
    @Transactional // ① GHI dữ liệu, nên cần nhãn riêng, không readOnly vì sẽ tạo lượt duyệt nếu
                   // chưa có
    public ReviewWorkspaceDTO openWorkspace(String email, Integer articleId) {
        // 1. Tìm người đăng nhập
        Account head = accounts.findByEmailIgnoreCase(email) // ①
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));

        // 2. Tìm bài, không có thì báo 404
        Article article = articles.findWithLeadAndTopic(articleId) // ②
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND)); // ③

        // 3. Kiểm tra quyền
        checkCanReview(head, article);

        // 4. Lấy các version, bản mới nhất là phần tử cuối
        List<ArticleVersion> allVersions = versions.findByArticleIdOrderByVersionNumberAsc(articleId);
        if (allVersions.isEmpty()) {
            throw new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED);
        }
        ArticleVersion current = allVersions.get(allVersions.size() - 1); // ④
        // previous = bản LIỀN TRƯỚC (phần tử áp chót); bài mới có 1 version thì null
        ArticleVersion previous = allVersions.size() > 1 ? allVersions.get(allVersions.size() - 2) : null;
        // 5. Mở lượt duyệt nếu đủ điều kiện // MỚI (cả khối dưới)
        boolean assignedToMe = assignments.isActiveOnTopic(
                article.getTopic().getId(), head.getId(), AssignmentRole.DEPARTMENT_HEAD);
        // Đọc là: tìm lượt duyệt đang mở trên bản này. Có rồi thì thôi, chưa có thì tạo
        // mới.
        // Nhờ vậy bấm F5 hay mở lại trang nhiều lần cũng không tạo thêm dòng mới.
        if (article.getStatus() == ArticleStatus.DEPARTMENT_REVIEW && assignedToMe) {
            articleReviews.findOpenReview(current.getId(), ReviewStage.DEPARTMENT)
                    .orElseGet(() -> articleReviews.save(newDepartmentReview(current, head)));
        }
        // Ghép tên các Supporting đang hoạt động trên Topic: "A, B, C"
        String supportingNames = "";
        for (Assignment assignment : assignments.findActiveByTopicAndRole(
                article.getTopic().getId(), AssignmentRole.SUPPORTING)) { // ①
            if (!supportingNames.isEmpty()) {
                supportingNames = supportingNames + ", ";
            }
            supportingNames = supportingNames + assignment.getAccount().getFullName();
        }
        // 6. Đóng gói cả trang vào một hộp
        return ReviewWorkspaceDTO.builder()
                .articleId(article.getId())
                .code("BT-" + article.getId())
                .topicTitle(article.getTopic().getTitle()) // ②
                .category(article.getCategory())
                .statusText(statusText(article.getStatus())) // ③
                .reviewerName(head.getFullName())
                .reviewerInitials(ReporterTaskMapper.initials(head.getFullName()))
                // chưa được gán phòng thì null, tránh lỗi NullPointerException
                .departmentName(head.getDepartment() != null ? head.getDepartment().getDepartmentName() : null)
                .leadName(article.getLead().getFullName()) // ④
                .leadInitials(ReporterTaskMapper.initials(article.getLead().getFullName()))
                .supportingNames(supportingNames)
                .current(mapper.toVersionView(current)) // bản đang duyệt
                .previous(previous != null ? mapper.toVersionView(previous) : null) // bản trước (có thể null)
                .comments(rootComments(current)) // góp ý trên bản đang duyệt
                .build();
    }

    @Override
    @Transactional // ① GHI dữ liệu, nên cần nhãn riêng, không readOnly
    public void approve(String email, Integer articleId, String feedback) {
        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        Article article = articles.findWithLeadAndTopic(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));

        // Duyệt phải có assignment (rule R09) và không phải Lead của bài (BR-34)
        ArticleReview review = myOpenReview(head, article);
        // Chốt lượt duyệt
        review.setDecision(ArticleReviewDecision.APPROVED); // ③
        // không có ghi chú → lưu NULL có ghi chú → bỏ khoảng trắng hai đầu rồi lưu
        review.setFeedback(feedback == null || feedback.isBlank() ? null : feedback.trim());
        review.setReviewedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        // Đổi trạng thái bài
        article.setStatus(ArticleStatus.DEPARTMENT_APPROVED); // ④
    }

    @Override
    @Transactional
    public void requestRevision(String email, Integer articleId, String feedback) {
        // Nhận xét là bắt buộc (BR-12)
        if (feedback == null || feedback.isBlank()) { // ①
            throw new ApplicationException(ErrorCode.REVIEW_FEEDBACK_REQUIRED);
        }

        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED)); // giống approve()
        Article article = articles.findWithLeadAndTopic(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND)); // giống approve()
        ArticleReview review = myOpenReview(head, article);

        review.setDecision(ArticleReviewDecision.REQUEST_REVISION); // ②
        review.setFeedback(feedback.trim());
        review.setReviewedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        article.setStatus(ArticleStatus.REVISION_REQUIRED); // ③
        article.setCompletedAt(null); // ck_article_completed: bài chưa APPROVED thì phải NULL
    }

    @Override
    @Transactional
    // Thêm góp ý chung cho cả bài (không gắn vị trí: các cột anchor_* để NULL)
    public void addComment(String email, Integer articleId, String content) {
        if (content == null || content.isBlank()) {
            throw new ApplicationException(ErrorCode.COMMENT_INVALID); // nội dung rỗng
        }
        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        Article article = articles.findWithLeadAndTopic(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        // Kiểm tra quyền GHI: được giao, không phải Lead, bài đang chờ duyệt; lấy lượt
        // duyệt đang mở
        ArticleReview review = myOpenReview(head, article);

        Comment comment = new Comment();
        comment.setReview(review); // thuộc lượt duyệt nào
        comment.setCreator(head); // ai viết
        comment.setContent(content.trim());
        comments.save(comment); // INSERT vào bảng comment
    }

    // Hàm phụ: góp ý GỐC trên một version (bỏ câu trả lời có parent), đổi thành
    // ReviewCommentDTO để hiển thị
    private List<ReviewCommentDTO> rootComments(ArticleVersion version) {
        List<ReviewCommentDTO> result = new ArrayList<>();
        // Chỉ lấy góp ý gốc (parent = null). Dự án không làm chức năng trả lời góp ý,
        // nhưng dữ liệu mẫu có sẵn vài câu trả lời của phóng viên, nên vẫn lọc ra để
        // không hiện lẫn vào.
        for (Comment comment : comments.findByVersion(version.getId())) { // đọc bảng comment theo mã version
            if (comment.getParent() == null) {
                result.add(mapper.toComment(comment, List.of()));
            }
        }
        return result;
    }

    // Hàm phụ hỗ trợ các hàm chính
    // Kiểm tra quyền + trạng thái, rồi trả về lượt duyệt của mình trên version mới
    // Trưởng phòng không được tự duyệt bài do chính mình phụ trách (BR-34)
    private ArticleReview myOpenReview(Account head, Article article) {

        boolean assignedToMe = assignments.isActiveOnTopic(
                article.getTopic().getId(), head.getId(), AssignmentRole.DEPARTMENT_HEAD);
        boolean isLead = article.getLead().getId().equals(head.getId());
        if (!assignedToMe || isLead) {
            throw new ApplicationException(ErrorCode.ARTICLE_REVIEW_FORBIDDEN);
        }

        // Chỉ bài đang chờ Trưởng phòng mới duyệt được
        if (article.getStatus() != ArticleStatus.DEPARTMENT_REVIEW) { // ②
            throw new ApplicationException(ErrorCode.ARTICLE_NOT_IN_DEPARTMENT_REVIEW);
        }

        // Lấy version mới nhất
        List<ArticleVersion> allVersions = versions.findByArticleIdOrderByVersionNumberAsc(article.getId());
        if (allVersions.isEmpty()) {
            throw new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED);
        }
        ArticleVersion current = allVersions.get(allVersions.size() - 1);

        // Lấy lượt duyệt đang mở, chưa có thì tạo mới
        ArticleReview review = articleReviews.findOpenReview(current.getId(), ReviewStage.DEPARTMENT)
                .orElseGet(() -> articleReviews.save(newDepartmentReview(current, head)));
        if (!review.getReviewer().getId().equals(head.getId())) {
            throw new ApplicationException(ErrorCode.ARTICLE_REVIEW_TAKEN);
        }
        return review;
    }

    // Được xem khi: Topic thuộc phòng mình HOẶC Topic được giao cho mình; và mình
    // không phải Lead của bài (BR-34)
    private void checkCanReview(Account head, Article article) {
        boolean assignedToMe = assignments.isActiveOnTopic(
                article.getTopic().getId(), head.getId(), AssignmentRole.DEPARTMENT_HEAD);

        Department headDepartment = head.getDepartment();
        Department topicDepartment = article.getTopic().getDepartment();
        boolean sameDepartment = headDepartment != null && topicDepartment != null
                && headDepartment.getId().equals(topicDepartment.getId());

        // người duyệt không được duyệt bài do chính mình phụ trách
        boolean isLead = article.getLead().getId().equals(head.getId());

        if ((!assignedToMe && !sameDepartment) || isLead) { // ⑤
            throw new ApplicationException(ErrorCode.ARTICLE_REVIEW_FORBIDDEN);
        }
    }

    private static ArticleReview newDepartmentReview(ArticleVersion version, Account head) {
        ArticleReview review = new ArticleReview();
        review.setVersion(version);
        review.setReviewer(head);
        review.setReviewStage(ReviewStage.DEPARTMENT);
        review.setReviewKind(ReviewKind.REVIEW);
        return review;
    }
        // Đổi trạng thái bài (enum) thành chữ tiếng Việt để hiển thị
    private static String statusText(ArticleStatus status) {
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case DEPARTMENT_REVIEW -> "Chờ Trưởng phòng duyệt";
            case REVISION_REQUIRED -> "Đã trả Lead chỉnh sửa";
            case DEPARTMENT_APPROVED -> "Đã duyệt cấp phòng · chờ Thư ký";
            case APPROVED -> "Thư ký đã duyệt cuối";
            case SCHEDULED -> "Đã hẹn giờ đăng";
            case PUBLISHED -> "Đã xuất bản";
        };
    }
}