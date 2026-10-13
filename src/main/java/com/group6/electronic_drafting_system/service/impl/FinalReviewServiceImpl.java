package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.review.FinalReviewQueueItemDTO;
import com.group6.electronic_drafting_system.dto.review.FinalReviewWorkspaceDTO;
import com.group6.electronic_drafting_system.dto.review.FinalReviewWorkspaceDTO.CommentItem;
import com.group6.electronic_drafting_system.dto.review.FinalReviewWorkspaceDTO.ReviewItem;
import com.group6.electronic_drafting_system.dto.review.FinalReviewWorkspaceDTO.SourceItem;
import com.group6.electronic_drafting_system.dto.review.FinalReviewWorkspaceDTO.TeamMember;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.ArticleSource;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Comment;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.CommentStatus;
import com.group6.electronic_drafting_system.entity.enums.ReviewKind;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleReviewRepository;
import com.group6.electronic_drafting_system.repository.ArticleSourceRepository;
import com.group6.electronic_drafting_system.repository.ArticleVersionRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.CommentRepository;
import com.group6.electronic_drafting_system.service.FinalReviewService;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalReviewServiceImpl implements FinalReviewService {
    static final int FEEDBACK_MAX_LENGTH = 4000;

    private final AccountRepository accounts;
    private final ArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final ArticleReviewRepository reviews;
    private final CommentRepository comments;
    private final AssignmentRepository assignments;
    private final ArticleSourceRepository articleSources;

    public FinalReviewServiceImpl(AccountRepository accounts, ArticleRepository articles, ArticleVersionRepository versions,
                                  ArticleReviewRepository reviews, CommentRepository comments,
                                  AssignmentRepository assignments, ArticleSourceRepository articleSources) {
        this.accounts = accounts;
        this.articles = articles;
        this.versions = versions;
        this.reviews = reviews;
        this.comments = comments;
        this.assignments = assignments;
        this.articleSources = articleSources;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinalReviewQueueItemDTO> getQueue(String secretaryEmail) {
        Account me = requireSecretary(secretaryEmail);
        List<Article> pending = articles.findByStatusOrderBySubmittedAtAsc(ArticleStatus.DEPARTMENT_APPROVED);
        if (pending.isEmpty()) return List.of();

        Map<Integer, ArticleVersion> latestByArticle = versions.findLatestByArticleIn(pending).stream()
                .collect(Collectors.toMap(v -> v.getArticle().getId(), Function.identity()));
        List<ArticleReview> latestReviews = reviews.findByVersionIn(latestByArticle.values());

        List<FinalReviewQueueItemDTO> items = new ArrayList<>();
        for (Article article : pending) {
            ArticleVersion version = latestByArticle.get(article.getId());
            if (version == null) continue;   // không xảy ra nếu dữ liệu đúng (R18)
            ArticleReview deptApproval = findReview(latestReviews, version, ReviewStage.DEPARTMENT, ArticleReviewDecision.APPROVED);
            ArticleReview openReview = findReview(latestReviews, version, ReviewStage.SECRETARY, null);
            items.add(FinalReviewQueueItemDTO.builder()
                    .articleId(article.getId())
                    .title(version.getTitle())
                    .topicTitle(article.getTopic().getTitle())
                    .category(article.getCategory())
                    .priority(article.getTopic().getPriority().name())
                    .leadName(article.getLead().getFullName())
                    .versionLabel(version.getVersionLabel())
                    .submittedAt(article.getSubmittedAt())
                    .departmentApprovedBy(deptApproval == null ? null : deptApproval.getReviewer().getFullName())
                    .departmentApprovedAt(deptApproval == null ? null : deptApproval.getReviewedAt())
                    .reviewingBy(openReview == null ? null : openReview.getReviewer().getFullName())
                    .reviewingByMe(openReview != null && sameAccount(openReview.getReviewer(), me))
                    .ownArticle(sameAccount(article.getLead(), me))
                    .build());
        }
        return items;
    }

    @Override
    @Transactional
    public FinalReviewWorkspaceDTO openWorkspace(Integer articleId, String secretaryEmail) {
        Account me = requireSecretary(secretaryEmail);
        Article article = articles.findWithTopicAndLeadById(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        ArticleVersion version = versions.findFirstByArticleOrderByVersionNumberDesc(article)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED));

        boolean canDecide = false;
        String blockedReason = null;
        if (article.getStatus() == ArticleStatus.DEPARTMENT_APPROVED) {
            if (sameAccount(article.getLead(), me)) {
                blockedReason = ErrorCode.REVIEW_OWN_ARTICLE.getMessage();
            } else if (!reviews.existsByVersionAndReviewStageAndDecision(version, ReviewStage.DEPARTMENT, ArticleReviewDecision.APPROVED)) {
                blockedReason = ErrorCode.REVIEW_DEPARTMENT_NOT_APPROVED.getMessage();
            } else {
                ArticleReview open = reviews.findByVersionAndReviewStageAndDecisionIsNull(version, ReviewStage.SECRETARY)
                        .orElseGet(() -> startReview(version, me));
                if (sameAccount(open.getReviewer(), me)) canDecide = true;
                else blockedReason = "Bài đang được " + open.getReviewer().getFullName() + " duyệt.";
            }
        }
        return buildWorkspace(article, version, canDecide, blockedReason);
    }

    @Override
    @Transactional
    public void approve(Integer articleId, String secretaryEmail, String feedback) {
        Account me = requireSecretary(secretaryEmail);
        Article article = lockPendingArticle(articleId, me);
        ArticleReview review = requireMyOpenReview(article, me);
        OffsetDateTime now = now();
        review.setDecision(ArticleReviewDecision.APPROVED);
        review.setFeedback(trimToNull(feedback));
        review.setReviewedAt(now);
        reviews.save(review);
        // Final Approve: chờ quyết định xuất bản (FL-07)
        article.setStatus(ArticleStatus.APPROVED);
        article.setCompletedAt(now);
        articles.save(article);
    }

    @Override
    @Transactional
    public void requestRevision(Integer articleId, String secretaryEmail, String feedback) {
        Account me = requireSecretary(secretaryEmail);
        String reason = trimToNull(feedback);
        if (reason == null) throw new ApplicationException(ErrorCode.REVIEW_FEEDBACK_REQUIRED);   // BR-12
        Article article = lockPendingArticle(articleId, me);
        ArticleReview review = requireMyOpenReview(article, me);
        review.setDecision(ArticleReviewDecision.REQUEST_REVISION);
        review.setFeedback(reason);
        review.setReviewedAt(now());
        reviews.save(review);
        // BR-11: trả thẳng Lead; sau khi Resubmit bài phải qua Trưởng phòng rồi mới tới Thư ký
        article.setStatus(ArticleStatus.REVISION_REQUIRED);
        article.setCompletedAt(null);
        articles.save(article);
    }

    private Account requireSecretary(String email) {
        Account account = email == null ? null : accounts.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (account == null || account.getStatus() != AccountStatus.ACTIVE
                || account.getRole().getRoleName() != RoleName.EDITORIAL_SECRETARY)
            throw new ApplicationException(ErrorCode.REVIEW_ACCESS_DENIED);
        return account;
    }

    private Article lockPendingArticle(Integer articleId, Account me) {
        Article article = articles.findByIdForUpdate(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        if (article.getStatus() != ArticleStatus.DEPARTMENT_APPROVED)
            throw new ApplicationException(ErrorCode.REVIEW_NOT_IN_FINAL_STAGE);
        if (sameAccount(article.getLead(), me)) throw new ApplicationException(ErrorCode.REVIEW_OWN_ARTICLE);   // BR-34
        return article;
    }

    private ArticleReview requireMyOpenReview(Article article, Account me) {
        ArticleVersion version = versions.findFirstByArticleOrderByVersionNumberDesc(article)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED));
        // BR-07 / R03: Thư ký chỉ duyệt bản đã được Trưởng phòng duyệt
        if (!reviews.existsByVersionAndReviewStageAndDecision(version, ReviewStage.DEPARTMENT, ArticleReviewDecision.APPROVED))
            throw new ApplicationException(ErrorCode.REVIEW_DEPARTMENT_NOT_APPROVED);
        ArticleReview review = reviews.findByVersionAndReviewStageAndDecisionIsNull(version, ReviewStage.SECRETARY)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_OPENED));
        if (!sameAccount(review.getReviewer(), me)) throw new ApplicationException(ErrorCode.REVIEW_HELD_BY_OTHER);
        return review;
    }

    private ArticleReview startReview(ArticleVersion version, Account me) {
        ArticleReview review = new ArticleReview();
        review.setVersion(version);
        review.setReviewer(me);
        review.setReviewStage(ReviewStage.SECRETARY);
        review.setReviewKind(ReviewKind.REVIEW);
        return reviews.saveAndFlush(review);
    }

    private FinalReviewWorkspaceDTO buildWorkspace(Article article, ArticleVersion version, boolean canDecide, String blockedReason) {
        List<ArticleSource> sources = articleSources.findByArticle(article).stream()
                .sorted(Comparator.comparing(ArticleSource::getFeatured).reversed()
                        .thenComparing(ArticleSource::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ArticleSource::getId))
                .toList();
        ArticleSource featured = sources.stream().filter(ArticleSource::getFeatured).findFirst().orElse(null);
        List<ArticleReview> decided = reviews.findByVersionArticleAndDecisionIsNotNullOrderByReviewedAtAscIdAsc(article);
        List<Comment> allComments = comments.findByReviewVersionArticleOrderByCreatedAtAscIdAsc(article);

        return FinalReviewWorkspaceDTO.builder()
                .articleId(article.getId())
                .status(article.getStatus().name())
                .statusLabel(articleStatusLabel(article.getStatus()))
                .versionLabel(version.getVersionLabel())
                .changeSummary(version.getChangeSummary())
                .submittedAt(version.getCreatedAt())
                .title(version.getTitle())
                .subtitle(version.getSubtitle())
                .sapo(version.getSapo())
                .paragraphs(splitParagraphs(version.getContent()))
                .category(article.getCategory())
                .featuredImageUrl(featured == null ? null : featured.getSource().getFileUrl())
                .featuredImageCaption(featured == null ? null : featured.getCaption())
                .topicTitle(article.getTopic().getTitle())
                .leadName(article.getLead().getFullName())
                .team(buildTeam(article))
                .sources(sources.stream().map(this::toSourceItem).toList())
                .revisionRounds(decided.stream().filter(r -> r.getDecision() == ArticleReviewDecision.REQUEST_REVISION).count())
                .reviewHistory(decided.stream().map(this::toReviewItem).toList())
                .currentVersionComments(buildComments(allComments, version, true))
                .earlierOpenComments(buildComments(allComments, version, false))
                .canDecide(canDecide)
                .blockedReason(blockedReason)
                .build();
    }

    private List<TeamMember> buildTeam(Article article) {
        return assignments.findByTopicAndStatusNot(article.getTopic(), AssignmentStatus.REMOVED).stream()
                .sorted(Comparator.comparing(Assignment::getAssignmentRole))   // DEPARTMENT_HEAD, LEAD, SUPPORTING
                .map(a -> new TeamMember(a.getAccount().getFullName(), assignmentRoleLabel(a.getAssignmentRole()),
                        a.getStatus() == AssignmentStatus.COMPLETED ? "Đã xong" : "Đang thực hiện",
                        a.getRequirements(), a.getDeadline()))
                .toList();
    }

    // currentVersion = true: mọi comment của version đang duyệt; false: comment gốc còn OPEN của các version trước
    private List<CommentItem> buildComments(List<Comment> all, ArticleVersion current, boolean currentVersion) {
        Map<Integer, List<Comment>> repliesByParent = all.stream().filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));
        return all.stream()
                .filter(c -> c.getParent() == null)
                .filter(c -> currentVersion
                        ? Objects.equals(c.getReview().getVersion().getId(), current.getId())
                        : !Objects.equals(c.getReview().getVersion().getId(), current.getId()) && c.getStatus() == CommentStatus.OPEN)
                .map(c -> toCommentItem(c, repliesByParent.getOrDefault(c.getId(), List.of()).stream()
                        .map(r -> toCommentItem(r, List.of())).toList()))
                .toList();
    }

    private CommentItem toCommentItem(Comment c, List<CommentItem> replies) {
        return new CommentItem(c.getReview().getVersion().getVersionLabel(), reviewStageLabel(c.getReview().getReviewStage()),
                anchorLabel(c), c.getQuotedText(), c.getContent(), c.getCreator().getFullName(), c.getStatus().name(),
                c.getCreatedAt(), replies);
    }

    private ReviewItem toReviewItem(ArticleReview r) {
        return new ReviewItem(r.getVersion().getVersionLabel(), reviewStageLabel(r.getReviewStage()), reviewKindLabel(r.getReviewKind()),
                r.getDecision().name(), r.getDecision() == ArticleReviewDecision.APPROVED ? "Đã duyệt" : "Yêu cầu sửa",
                r.getReviewer().getFullName(), r.getFeedback(), r.getReviewedAt());
    }

    private SourceItem toSourceItem(ArticleSource as) {
        SourceType type = as.getSource().getSourceType();
        return new SourceItem(as.getSource().getTitle(), sourceTypeLabel(type), as.getSource().getFileUrl(), as.getCaption(),
                as.getNote(), Boolean.TRUE.equals(as.getFeatured()),
                type == SourceType.INTERVIEW || type == SourceType.INTERNAL_DOC);
    }

    private static ArticleReview findReview(List<ArticleReview> list, ArticleVersion version, ReviewStage stage, ArticleReviewDecision decision) {
        return list.stream()
                .filter(r -> Objects.equals(r.getVersion().getId(), version.getId()) && r.getReviewStage() == stage
                        && r.getDecision() == decision)
                .findFirst().orElse(null);
    }

    // Các đoạn văn cách nhau bằng một dòng trống (quy ước của bảng article_version)
    private static List<String> splitParagraphs(String content) {
        if (content == null || content.isBlank()) return List.of();
        return Arrays.stream(content.strip().split("\\R\\s*\\R")).map(String::strip).filter(p -> !p.isEmpty()).toList();
    }

    private static boolean sameAccount(Account a, Account b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }

    private static String trimToNull(String text) {
        if (text == null || text.isBlank()) return null;
        String trimmed = text.strip();
        return trimmed.length() > FEEDBACK_MAX_LENGTH ? trimmed.substring(0, FEEDBACK_MAX_LENGTH) : trimmed;
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static String anchorLabel(Comment c) {
        AnchorField field = c.getAnchorField();
        if (field == null) return "Góp ý chung";
        return switch (field) {
            case TITLE -> "Tít";
            case SUBTITLE -> "Tít phụ";
            case SAPO -> "Sapo";
            case CONTENT -> c.getAnchorParagraph() == null ? "Nội dung" : "Đoạn " + c.getAnchorParagraph();
        };
    }

    private static String articleStatusLabel(ArticleStatus status) {
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case DEPARTMENT_REVIEW -> "Chờ Trưởng phòng duyệt";
            case REVISION_REQUIRED -> "Đã trả về Lead Reporter";
            case DEPARTMENT_APPROVED -> "Chờ Thư ký duyệt cuối";
            case APPROVED -> "Đã duyệt cuối, chờ xuất bản";
            case SCHEDULED -> "Đã hẹn giờ đăng";
            case PUBLISHED -> "Đã xuất bản";
        };
    }

    private static String reviewStageLabel(ReviewStage stage) {
        return stage == ReviewStage.DEPARTMENT ? "Trưởng phòng" : "Thư ký tòa soạn";
    }

    private static String reviewKindLabel(ReviewKind kind) {
        return switch (kind) {
            case REVIEW -> "Duyệt bài";
            case RETURN_TO_LEAD -> "Trả bài đã duyệt";
            case CANCEL_SCHEDULE -> "Hủy lịch đăng";
        };
    }

    private static String assignmentRoleLabel(AssignmentRole role) {
        return switch (role) {
            case DEPARTMENT_HEAD -> "Trưởng phòng phụ trách";
            case LEAD -> "Lead Reporter";
            case SUPPORTING -> "Supporting Reporter";
        };
    }

    private static String sourceTypeLabel(SourceType type) {
        return switch (type) {
            case IMAGE -> "Hình ảnh";
            case DOCUMENT -> "Tài liệu";
            case WEBSITE -> "Website";
            case INTERVIEW -> "Phỏng vấn";
            case INTERNAL_DOC -> "Tài liệu nội bộ";
        };
    }
}
