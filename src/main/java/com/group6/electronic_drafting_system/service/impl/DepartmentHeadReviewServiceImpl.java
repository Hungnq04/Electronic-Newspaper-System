package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewCommentDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Comment;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.ReviewWorkspaceMapper;
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
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
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
    public VersionViewDTO openArticle(String email, Integer articleId) {
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
        // 5. Đổi version thành DTO để hiển thị
        return mapper.toVersionView(current);
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
    public void addComment(String email, Integer articleId, String content, String anchorField, Integer anchorParagraph,
            Integer anchorStart, Integer anchorEnd) {
        if (content == null || content.isBlank()) {
            throw new ApplicationException(ErrorCode.COMMENT_INVALID); // ① lỗi "góp ý không hợp lệ"
        }
        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED)); // giống approve()
        Article article = articles.findWithLeadAndTopic(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND)); // giống approve()
        ArticleReview review = myOpenReview(head, article);

        Comment comment = new Comment();
        comment.setReview(review); // ② thuộc lượt duyệt nào
        comment.setCreator(head); // ③ ai viết
        comment.setContent(content.trim());
        // Góp ý gắn vị trí: kiểm tra vị trí có thật, quoted_text lấy từ DB (không tin
        // chữ gửi lên)
        if (anchorField != null && !anchorField.isBlank()) {
            AnchorField field;
            try {
                field = AnchorField.valueOf(anchorField); // "CONTENT" (chữ) → AnchorField.CONTENT (enum)
            } catch (IllegalArgumentException ex) {
                throw new ApplicationException(ErrorCode.COMMENT_INVALID);
            }
            int paragraph = anchorParagraph == null ? 0 : anchorParagraph;

            // Lấy chữ THẬT của đoạn đó trong version đang duyệt
            String text = mapper.anchorText(review.getVersion(), field, paragraph);
            if (text == null || anchorStart == null || anchorEnd == null
                    || anchorStart < 0 || anchorEnd <= anchorStart || anchorEnd > text.length()) {
                throw new ApplicationException(ErrorCode.COMMENT_INVALID);
            }

            comment.setAnchorField(field);
            comment.setAnchorParagraph(paragraph);
            comment.setAnchorStart(anchorStart); // ①
            comment.setAnchorEnd(anchorEnd); // ②
            comment.setQuotedText(text.substring(anchorStart, anchorEnd)); // cắt đúng đoạn chữ từ DB
        }
        comments.save(comment);  
    }

    @Override
    public List<ReviewCommentDTO> getComments(String email, Integer articleId) {
        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED)); // giống approve()
        Article article = articles.findWithLeadAndTopic(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND)); // giống approve()
        checkCanReview(head, article);

        List<ArticleVersion> allVersions = versions.findByArticleIdOrderByVersionNumberAsc(articleId);
        if (allVersions.isEmpty()) {
            return List.of();
        }
        ArticleVersion current = allVersions.get(allVersions.size() - 1);

        List<ReviewCommentDTO> result = new ArrayList<>();
        for (Comment comment : comments.findByVersion(current.getId())) {
            if (comment.getParent() == null) { // chỉ lấy góp ý gốc, bỏ câu trả lời
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
}