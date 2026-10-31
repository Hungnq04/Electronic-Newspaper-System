package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.DepartmentHead.CommentRequestDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewCommentDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewWorkspaceDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.SourceItemDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Comment;
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.CommentStatus;
import com.group6.electronic_drafting_system.entity.enums.ReviewKind;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.DraftEntryMapper;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.ReviewWorkspaceMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleReviewRepository;
import com.group6.electronic_drafting_system.repository.ArticleVersionRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.CommentRepository;
import com.group6.electronic_drafting_system.service.DepartmentHeadReviewService;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartmentHeadReviewServiceImpl implements DepartmentHeadReviewService {
    private final AccountRepository accounts;
    private final ArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final ArticleReviewRepository reviews;
    private final CommentRepository comments;
    private final AssignmentRepository assignments;
    private final ReviewWorkspaceMapper mapper;
    private final DraftEntryMapper entryMapper;

    public DepartmentHeadReviewServiceImpl(AccountRepository accounts, ArticleRepository articles,
                                           ArticleVersionRepository versions, ArticleReviewRepository reviews,
                                           CommentRepository comments, AssignmentRepository assignments,
                                           ReviewWorkspaceMapper mapper, DraftEntryMapper entryMapper) {
        this.accounts = accounts;
        this.articles = articles;
        this.versions = versions;
        this.reviews = reviews;
        this.comments = comments;
        this.assignments = assignments;
        this.mapper = mapper;
        this.entryMapper = entryMapper;
    }

    // Không readOnly: lần đầu mở bài sẽ tạo lượt duyệt (article_review, decision = NULL) - CLAUDE.md mục 7 "Review"
    @Override
    @Transactional
    public ReviewWorkspaceDTO openWorkspace(String email, Integer articleId) {
        Account head = currentHead(email);
        Article article = articleOfHead(head, articleId);
        List<ArticleVersion> allVersions = versions.findByArticleIdOrderByVersionNumberAsc(articleId);
        if (allVersions.isEmpty()) throw new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED);
        ArticleVersion current = allVersions.get(allVersions.size() - 1);
        ArticleVersion previous = allVersions.size() > 1 ? allVersions.get(allVersions.size() - 2) : null;

        // Chỉ bài đang DEPARTMENT_REVIEW mới mở lượt duyệt; bài đã duyệt / đã trả thì chỉ xem
        boolean reviewable = false;
        if (article.getStatus() == ArticleStatus.DEPARTMENT_REVIEW) {
            ArticleReview open = reviews.findOpenReview(current.getId(), ReviewStage.DEPARTMENT)
                    .orElseGet(() -> reviews.save(newDepartmentReview(current, head)));
            reviewable = open.getReviewer().getId().equals(head.getId());
        }

        // Thân bài theo bố cục phóng viên; link ảnh / video trong bài dùng để đánh dấu tư liệu "Có trong bài"
        VersionViewDTO currentView = mapper.toVersionView(current);
        Set<String> bodyUrls = mapper.mediaUrls(currentView.getBlocks());
        List<SourceItemDTO> sources = articles.findSources(articleId).stream()
                .map(source -> mapper.toSource(source, bodyUrls)).toList();
        // Ảnh đại diện: mỗi bài tối đa một (uq_article_featured), luôn là Source loại IMAGE (R16)
        SourceItemDTO featuredImage = sources.stream()
                .filter(source -> source.isFeatured() && source.getUrl() != null).findFirst().orElse(null);
        String supportingNames = assignments.findActiveByTopicAndRole(article.getTopic().getId(), AssignmentRole.SUPPORTING)
                .stream().map(s -> s.getAccount().getFullName()).collect(Collectors.joining(", "));

        return ReviewWorkspaceDTO.builder()
                .articleId(article.getId())
                .code("BT-" + article.getId())
                .topicTitle(article.getTopic().getTitle())
                .category(article.getCategory())
                .statusText(statusText(article.getStatus()))
                .reviewable(reviewable)
                .reviewerName(head.getFullName())
                .reviewerInitials(entryMapper.initials(head.getFullName()))
                .departmentName(head.getDepartment() != null ? head.getDepartment().getDepartmentName() : null)
                .leadName(article.getLead().getFullName())
                .leadInitials(entryMapper.initials(article.getLead().getFullName()))
                .supportingNames(supportingNames)
                .wordCount(String.format(Locale.forLanguageTag("vi-VN"), "%,d từ", countWords(current.getContent())))
                .imageCount((int) sources.stream().filter(SourceItemDTO::isImage).count())
                .current(currentView)
                .previous(previous != null ? mapper.toVersionView(previous) : null)
                .versions(allVersions.stream().map(mapper::toVersionView).toList())
                .sources(sources)
                .featuredImage(featuredImage)
                .comments(rootComments(current))
                .previousComments(previous != null ? rootComments(previous) : List.of())
                .build();
    }

    @Override
    @Transactional
    public void addComment(String email, Integer articleId, CommentRequestDTO request) {
        Account head = currentHead(email);
        ArticleReview review = myOpenReview(head, articleOfHead(head, articleId));
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) throw new ApplicationException(ErrorCode.COMMENT_INVALID);

        Comment comment = new Comment();
        comment.setReview(review);
        comment.setCreator(head); // R17: comment gốc phải do chính người duyệt viết
        comment.setContent(content);

        // Góp ý gắn vị trí: kiểm tra vị trí có thật trong version đang duyệt, quoted_text lấy từ DB (không tin chữ gửi lên)
        if (request.getAnchorField() != null && !request.getAnchorField().isBlank()) {
            AnchorField field = parseAnchorField(request.getAnchorField());
            int paragraph = request.getAnchorParagraph() == null ? 0 : request.getAnchorParagraph();
            String text = mapper.anchorText(review.getVersion(), field, paragraph);
            Integer start = request.getAnchorStart(), end = request.getAnchorEnd();
            if (text == null || start == null || end == null || start < 0 || end <= start || end > text.length()) {
                throw new ApplicationException(ErrorCode.COMMENT_INVALID);
            }
            comment.setAnchorField(field);
            comment.setAnchorParagraph(paragraph);
            comment.setAnchorStart(start);
            comment.setAnchorEnd(end);
            comment.setQuotedText(text.substring(start, end));
        }
        comments.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(String email, Integer articleId, Integer commentId) {
        Account head = currentHead(email);
        ArticleReview review = myOpenReview(head, articleOfHead(head, articleId));
        Comment comment = comments.findWithReview(commentId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.COMMENT_NOT_FOUND));
        // Chỉ xóa được góp ý của chính mình, trong lượt duyệt chưa chốt và chưa có ai trả lời
        if (!comment.getReview().getId().equals(review.getId()) || !comment.getCreator().getId().equals(head.getId())
                || !comment.getReplies().isEmpty()) {
            throw new ApplicationException(ErrorCode.COMMENT_INVALID);
        }
        comments.delete(comment);
    }

    @Override
    @Transactional
    public void setPreviousCommentResolved(String email, Integer articleId, Integer commentId, boolean resolved) {
        Account head = currentHead(email);
        ArticleReview review = myOpenReview(head, articleOfHead(head, articleId));
        Comment comment = comments.findWithReview(commentId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.COMMENT_NOT_FOUND));
        // Góp ý phải thuộc một version CŨ của chính bài này (vòng trước)
        ArticleVersion version = comment.getReview().getVersion();
        if (!version.getArticle().getId().equals(articleId) || version.getId().equals(review.getVersion().getId())
                || comment.getParent() != null) {
            throw new ApplicationException(ErrorCode.COMMENT_INVALID);
        }
        comment.setStatus(resolved ? CommentStatus.RESOLVED : CommentStatus.OPEN);
    }

    // DEPARTMENT_REVIEW -> DEPARTMENT_APPROVED (CLAUDE.md mục 6.2). Đây KHÔNG phải Final Approve; bài sang hàng chờ Thư ký
    @Override
    @Transactional
    public void approve(String email, Integer articleId, String feedback) {
        Account head = currentHead(email);
        Article article = articleOfHead(head, articleId);
        ArticleReview review = myOpenReview(head, article);
        String note = feedback == null || feedback.isBlank() ? null : feedback.trim();
        closeReview(review, ArticleReviewDecision.APPROVED, note);
        article.setStatus(ArticleStatus.DEPARTMENT_APPROVED);
    }

    // DEPARTMENT_REVIEW -> REVISION_REQUIRED, bắt buộc có nhận xét (BR-12, ràng buộc ck_review_feedback)
    @Override
    @Transactional
    public void requestRevision(String email, Integer articleId, String feedback) {
        if (feedback == null || feedback.isBlank()) throw new ApplicationException(ErrorCode.REVIEW_FEEDBACK_REQUIRED);
        Account head = currentHead(email);
        Article article = articleOfHead(head, articleId);
        ArticleReview review = myOpenReview(head, article);
        closeReview(review, ArticleReviewDecision.REQUEST_REVISION, feedback.trim());
        article.setStatus(ArticleStatus.REVISION_REQUIRED);
        article.setCompletedAt(null); // ck_article_completed: bài chưa APPROVED thì completed_at phải NULL
    }

    // ---------- Kiểm tra quyền ----------

    private Account currentHead(String email) {
        return accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
    }

    // Trưởng phòng chỉ được xem / duyệt bài thuộc Topic mà mình đang có assignment DEPARTMENT_HEAD (R09),
    // và không được duyệt bài do chính mình phụ trách (BR-34)
    private Article articleOfHead(Account head, Integer articleId) {
        Article article = articles.findWithLeadAndTopic(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        boolean isHeadOfTopic = assignments.isActiveOnTopic(article.getTopic().getId(), head.getId(), AssignmentRole.DEPARTMENT_HEAD);
        if (!isHeadOfTopic || article.getLead().getId().equals(head.getId())) {
            throw new ApplicationException(ErrorCode.ARTICLE_REVIEW_FORBIDDEN);
        }
        return article;
    }

    // Lượt duyệt đang mở trên version mới nhất, và phải là của người đang thao tác
    private ArticleReview myOpenReview(Account head, Article article) {
        if (article.getStatus() != ArticleStatus.DEPARTMENT_REVIEW) {
            throw new ApplicationException(ErrorCode.ARTICLE_NOT_IN_DEPARTMENT_REVIEW);
        }
        List<ArticleVersion> allVersions = versions.findByArticleIdOrderByVersionNumberAsc(article.getId());
        if (allVersions.isEmpty()) throw new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED);
        ArticleVersion current = allVersions.get(allVersions.size() - 1);
        ArticleReview review = reviews.findOpenReview(current.getId(), ReviewStage.DEPARTMENT)
                .orElseGet(() -> reviews.save(newDepartmentReview(current, head)));
        if (!review.getReviewer().getId().equals(head.getId())) {
            throw new ApplicationException(ErrorCode.ARTICLE_REVIEW_TAKEN);
        }
        return review;
    }

    // ---------- Hàm phụ ----------

    private static ArticleReview newDepartmentReview(ArticleVersion version, Account head) {
        ArticleReview review = new ArticleReview();
        review.setVersion(version);
        review.setReviewer(head);
        review.setReviewStage(ReviewStage.DEPARTMENT);
        review.setReviewKind(ReviewKind.REVIEW);
        return review;
    }

    // decision và reviewed_at phải cùng có giá trị (ck_review_decided)
    private static void closeReview(ArticleReview review, ArticleReviewDecision decision, String feedback) {
        review.setDecision(decision);
        review.setFeedback(feedback);
        review.setReviewedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    // Góp ý gốc của mọi lượt duyệt trên một version, mỗi góp ý kèm các câu trả lời của nó
    private List<ReviewCommentDTO> rootComments(ArticleVersion version) {
        List<Comment> all = comments.findByVersion(version.getId());
        return all.stream()
                .filter(c -> c.getParent() == null)
                .map(root -> mapper.toComment(root, all.stream()
                        .filter(c -> c.getParent() != null && c.getParent().getId().equals(root.getId())).toList()))
                .toList();
    }

    private static AnchorField parseAnchorField(String value) {
        try {
            return AnchorField.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ApplicationException(ErrorCode.COMMENT_INVALID);
        }
    }

    // Nội dung có thể là HTML từ trình soạn thảo: bỏ thẻ trước rồi mới đếm, để không đếm "<p>", "<img ...>" thành chữ
    private static int countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        String text = Jsoup.parse(content).text().trim();
        return text.isEmpty() ? 0 : text.split("\\s+").length;
    }

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
