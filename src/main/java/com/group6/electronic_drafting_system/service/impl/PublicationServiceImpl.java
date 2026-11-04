package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Publication.ApprovedArticleItemDTO;
import com.group6.electronic_drafting_system.dto.Publication.PublicationDetailDTO;
import com.group6.electronic_drafting_system.dto.Publication.ScheduledArticleItemDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.ArticleSource;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Publication;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.PublicationStatus;
import com.group6.electronic_drafting_system.entity.enums.PublicationType;
import com.group6.electronic_drafting_system.entity.enums.ReviewKind;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleReviewRepository;
import com.group6.electronic_drafting_system.repository.ArticleSourceRepository;
import com.group6.electronic_drafting_system.repository.ArticleVersionRepository;
import com.group6.electronic_drafting_system.repository.PublicationRepository;
import com.group6.electronic_drafting_system.service.PublicationService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicationServiceImpl implements PublicationService {
    // Mọi thời điểm lưu theo giờ Việt Nam (+07:00), đúng quy ước cột DATETIMEOFFSET của DB
    private static final ZoneOffset VN = ZoneOffset.ofHours(7);
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final AccountRepository accounts;
    private final ArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final ArticleReviewRepository reviews;
    private final ArticleSourceRepository articleSources;
    private final PublicationRepository publications;

    public PublicationServiceImpl(AccountRepository accounts, ArticleRepository articles, ArticleVersionRepository versions,
                                  ArticleReviewRepository reviews, ArticleSourceRepository articleSources,
                                  PublicationRepository publications) {
        this.accounts = accounts;
        this.articles = articles;
        this.versions = versions;
        this.reviews = reviews;
        this.articleSources = articleSources;
        this.publications = publications;
    }

    // ------------------------------------------------------------------ Màn hình

    @Override
    @Transactional(readOnly = true)
    public List<ApprovedArticleItemDTO> getApprovedArticles(String secretaryEmail) {
        requireSecretary(secretaryEmail);
        List<Article> approved = articles.findByStatusOrderBySubmittedAtAsc(ArticleStatus.APPROVED);

        List<ApprovedArticleItemDTO> items = new ArrayList<>();
        for (Article article : approved) {
            ArticleVersion version = versions.findFirstByArticleOrderByVersionNumberDesc(article).orElse(null);
            if (version == null) continue;   // không xảy ra nếu dữ liệu đúng (R18)
            ArticleReview finalApproval = findDecision(article, version, ReviewStage.SECRETARY);
            items.add(ApprovedArticleItemDTO.builder()
                    .articleId(article.getId())
                    .title(version.getTitle())
                    .topicTitle(article.getTopic().getTitle())
                    .category(article.getCategory())
                    .leadName(article.getLead().getFullName())
                    .versionLabel(version.getVersionLabel())
                    .finalApprovedBy(finalApproval == null ? null : finalApproval.getReviewer().getFullName())
                    .finalApprovedAt(finalApproval == null ? null : finalApproval.getReviewedAt())
                    .build());
        }
        // Bài được duyệt cuối sớm nhất đứng đầu (chờ lâu nhất)
        items.sort(Comparator.comparing(ApprovedArticleItemDTO::getFinalApprovedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicationDetailDTO getDetail(Integer articleId, String secretaryEmail) {
        requireSecretary(secretaryEmail);
        Article article = articles.findWithTopicAndLeadById(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        // Màn này chỉ dành cho bài đã duyệt cuối (chờ đăng / đã hẹn giờ / đã đăng)
        if (article.getStatus() != ArticleStatus.APPROVED && article.getStatus() != ArticleStatus.SCHEDULED
                && article.getStatus() != ArticleStatus.PUBLISHED)
            throw new ApplicationException(ErrorCode.PUBLICATION_NOT_APPROVED);
        return buildDetail(article);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicationDetailDTO getScheduledDetail(Integer articleId, String secretaryEmail) {
        requireSecretary(secretaryEmail);
        Article article = articles.findWithTopicAndLeadById(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        // Màn #68 chỉ dành cho bài đang hẹn giờ (đã đăng / đã hủy lịch thì không mở được nữa)
        if (article.getStatus() != ArticleStatus.SCHEDULED)
            throw new ApplicationException(ErrorCode.PUBLICATION_NOT_SCHEDULED);
        return buildDetail(article);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledArticleItemDTO> getScheduledArticles(String secretaryEmail) {
        requireSecretary(secretaryEmail);
        OffsetDateTime now = now();
        List<ScheduledArticleItemDTO> items = new ArrayList<>();
        for (Publication p : publications.findWithArticleByArticleStatus(ArticleStatus.SCHEDULED)) {
            items.add(ScheduledArticleItemDTO.builder()
                    .articleId(p.getArticle().getId())
                    .title(p.getVersion().getTitle())               // tít của đúng bản sẽ đăng
                    .topicTitle(p.getArticle().getTopic().getTitle())
                    .category(p.getArticle().getCategory())
                    .leadName(p.getArticle().getLead().getFullName())
                    .versionLabel(p.getVersion().getVersionLabel())
                    .scheduledAt(p.getScheduledAt())
                    .scheduledBy(p.getPublishedBy().getFullName())
                    .due(!p.getScheduledAt().isAfter(now))
                    .build());
        }
        return items;
    }

    // Dữ liệu chung cho màn #62 Publication Detail và #68 Scheduled Publication Detail
    private PublicationDetailDTO buildDetail(Article article) {
        Integer articleId = article.getId();
        ArticleVersion version = versions.findFirstByArticleOrderByVersionNumberDesc(article)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED));
        Publication publication = publications.findByArticle_Id(articleId).orElse(null);
        // Bài đã có lịch / đã đăng thì hiển thị đúng version được ghim trong publication
        if (publication != null) version = publication.getVersion();

        ArticleReview departmentApproval = findDecision(article, version, ReviewStage.DEPARTMENT);
        ArticleReview finalApproval = findDecision(article, version, ReviewStage.SECRETARY);
        ArticleSource featured = articleSources.findByArticle(article).stream()
                .filter(s -> Boolean.TRUE.equals(s.getFeatured())).findFirst().orElse(null);
        boolean scheduled = article.getStatus() == ArticleStatus.SCHEDULED && publication != null;
        // Đã đến giờ hẹn nhưng job (chạy mỗi phút) chưa kịp đăng
        boolean due = scheduled && !publication.getScheduledAt().isAfter(now());

        return PublicationDetailDTO.builder()
                .articleId(article.getId())
                .status(article.getStatus().name())
                .statusLabel(statusLabel(article.getStatus()))
                .versionLabel(version.getVersionLabel())
                .title(version.getTitle())
                .subtitle(version.getSubtitle())
                .sapo(version.getSapo())
                .paragraphs(splitParagraphs(version.getContent()))
                .category(article.getCategory())
                .featuredImageUrl(featured == null ? null : featured.getSource().getFileUrl())
                .featuredImageCaption(featured == null ? null : featured.getCaption())
                .topicTitle(article.getTopic().getTitle())
                .leadName(article.getLead().getFullName())
                .departmentApprovedBy(departmentApproval == null ? null : departmentApproval.getReviewer().getFullName())
                .departmentApprovedAt(departmentApproval == null ? null : departmentApproval.getReviewedAt())
                .finalApprovedBy(finalApproval == null ? null : finalApproval.getReviewer().getFullName())
                .finalApprovedAt(finalApproval == null ? null : finalApproval.getReviewedAt())
                .finalFeedback(finalApproval == null ? null : finalApproval.getFeedback())
                .publicationTypeLabel(publication == null ? null
                        : publication.getPublicationType() == PublicationType.IMMEDIATE ? "Đăng ngay" : "Hẹn giờ")
                .scheduledAt(publication == null ? null : publication.getScheduledAt())
                .publishedAt(publication == null ? null : publication.getPublishedAt())
                .publishedBy(publication == null ? null : publication.getPublishedBy().getFullName())
                .canPublish(article.getStatus() == ArticleStatus.APPROVED)
                .canCancel(scheduled && !due)
                .due(due)
                .minScheduleAt(now().plusMinutes(MIN_SCHEDULE_MINUTES + 1).truncatedTo(ChronoUnit.MINUTES).format(INPUT_FORMAT))
                .build();
    }

    // ------------------------------------------------------------------ Đăng ngay / Hẹn giờ

    @Override
    @Transactional   // ghi 2 bảng (publication + article): lỗi giữa chừng thì hủy cả hai
    public void publishNow(Integer articleId, String secretaryEmail) {
        Account me = requireSecretary(secretaryEmail);
        Article article = lockApprovedArticle(articleId);
        ArticleVersion version = requireFinalApprovedVersion(article);
        OffsetDateTime now = now();

        Publication publication = new Publication();
        publication.setArticle(article);
        publication.setVersion(version);                       // R05: đúng bản Thư ký đã duyệt cuối
        publication.setPublishedBy(me);
        publication.setPublicationType(PublicationType.IMMEDIATE);
        publication.setScheduledAt(null);                      // ck_publication_schedule: đăng ngay thì không có giờ hẹn
        publication.setStatus(PublicationStatus.PUBLISHED);
        publication.setPublishedAt(now);
        publications.save(publication);

        // Đổi trạng thái: APPROVED -> PUBLISHED (R06: khớp với publication.status)
        article.setStatus(ArticleStatus.PUBLISHED);
        if (article.getCompletedAt() == null) article.setCompletedAt(now);   // ck_article_completed
        articles.save(article);
    }

    @Override
    @Transactional   // ghi 2 bảng (publication + article)
    public void schedule(Integer articleId, String secretaryEmail, String scheduledAtText) {
        Account me = requireSecretary(secretaryEmail);
        OffsetDateTime scheduledAt = parseScheduleTime(scheduledAtText);
        // FR-12: thời điểm đăng phải ở tương lai (nhóm chốt: cách hiện tại ít nhất 5 phút)
        if (scheduledAt.isBefore(now().plusMinutes(MIN_SCHEDULE_MINUTES)))
            throw new ApplicationException(ErrorCode.PUBLICATION_SCHEDULE_TIME_TOO_SOON);

        Article article = lockApprovedArticle(articleId);
        ArticleVersion version = requireFinalApprovedVersion(article);

        Publication publication = new Publication();
        publication.setArticle(article);
        publication.setVersion(version);
        publication.setPublishedBy(me);                        // với bài hẹn giờ: Thư ký đã đặt lịch
        publication.setPublicationType(PublicationType.SCHEDULED);
        publication.setScheduledAt(scheduledAt);
        publication.setStatus(PublicationStatus.SCHEDULED);
        publication.setPublishedAt(null);                      // chưa đăng thì chưa có giờ đăng
        publications.save(publication);

        // Đổi trạng thái: APPROVED -> SCHEDULED
        article.setStatus(ArticleStatus.SCHEDULED);
        if (article.getCompletedAt() == null) article.setCompletedAt(now());
        articles.save(article);
    }

    // ------------------------------------------------------------------ Job tự đăng (UC-32)

    @Override
    @Transactional(readOnly = true)
    public List<Integer> findDueScheduledArticleIds() {
        return publications.findDueArticleIds(PublicationStatus.SCHEDULED, now());
    }

    @Override
    @Transactional   // mỗi bài một transaction riêng: bài này lỗi không ảnh hưởng bài khác
    public boolean publishScheduled(Integer articleId) {
        // Khóa dòng bài: nếu Thư ký đang hủy lịch cùng lúc thì một trong hai phải chờ bên kia xong
        Article article = articles.findByIdForUpdate(articleId).orElse(null);
        // Chỉ đăng nếu bài VẪN CÒN SCHEDULED (đã bị hủy lịch / trả về Lead thì bỏ qua)
        if (article == null || article.getStatus() != ArticleStatus.SCHEDULED) return false;

        Publication publication = publications.findByArticle_Id(articleId).orElse(null);
        if (publication == null || publication.getStatus() != PublicationStatus.SCHEDULED) return false;
        OffsetDateTime now = now();
        if (publication.getScheduledAt().isAfter(now)) return false;   // chưa đến giờ

        publication.setStatus(PublicationStatus.PUBLISHED);
        publication.setPublishedAt(now);                       // ck_publication_order: published_at >= scheduled_at
        publications.save(publication);

        // Đổi trạng thái: SCHEDULED -> PUBLISHED
        article.setStatus(ArticleStatus.PUBLISHED);
        articles.save(article);
        return true;
    }

    // ------------------------------------------------------------------ Trả bài về Lead (Iter3)

    @Override
    @Transactional   // ghi 2 bảng (article_review + article): lỗi giữa chừng thì hủy cả hai
    public void returnToLead(Integer articleId, String secretaryEmail, String reason) {
        Account me = requireSecretary(secretaryEmail);
        String feedback = requireReason(reason);              // BR-30: lý do bắt buộc
        Article article = lockApprovedArticle(articleId);     // chỉ bài APPROVED (chưa đăng, chưa hẹn giờ)
        ArticleVersion version = versions.findFirstByArticleOrderByVersionNumberDesc(article)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED));

        // Ghi một lượt Thư ký "trả bài đã duyệt" trên phiên bản đã duyệt cuối
        saveReturnReview(version, me, ReviewKind.RETURN_TO_LEAD, feedback);

        // Đổi trạng thái: APPROVED -> REVISION_REQUIRED, bài quay về Lead
        returnArticleToLead(article);
    }

    @Override
    @Transactional   // ghi 3 bảng (article_review + publication + article): lỗi giữa chừng thì hủy tất cả
    public void cancelAndReturn(Integer articleId, String secretaryEmail, String reason) {
        Account me = requireSecretary(secretaryEmail);
        String feedback = requireReason(reason);              // BR-30: lý do bắt buộc

        // Khóa dòng bài: nếu job tự đăng chạy đúng lúc này thì một trong hai phải chờ bên kia xong
        Article article = articles.findByIdForUpdate(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        if (article.getStatus() != ArticleStatus.SCHEDULED)
            throw new ApplicationException(ErrorCode.PUBLICATION_NOT_SCHEDULED);
        Publication publication = publications.findByArticle_Id(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.PUBLICATION_NOT_SCHEDULED));
        // UC-31: chỉ hủy được TRƯỚC giờ đăng
        if (!publication.getScheduledAt().isAfter(now()))
            throw new ApplicationException(ErrorCode.PUBLICATION_SCHEDULE_ALREADY_DUE);

        // Ghi một lượt Thư ký "hủy lịch đăng" trên đúng phiên bản đã được hẹn đăng
        saveReturnReview(publication.getVersion(), me, ReviewKind.CANCEL_SCHEDULE, feedback);

        // BR-31: hủy lịch = xóa dòng publication (cùng transaction, nếu không v_rule_violations báo R06)
        article.setPublication(null);
        publications.delete(publication);

        // Đổi trạng thái: SCHEDULED -> REVISION_REQUIRED, bài quay về Lead
        returnArticleToLead(article);
    }

    // Thêm một dòng article_review: cấp SECRETARY, kết quả REQUEST_REVISION, kèm lý do
    // (ràng buộc ck_review_kind: RETURN_TO_LEAD / CANCEL_SCHEDULE luôn là SECRETARY + REQUEST_REVISION)
    private void saveReturnReview(ArticleVersion version, Account secretary, ReviewKind kind, String feedback) {
        ArticleReview review = new ArticleReview();
        review.setVersion(version);
        review.setReviewer(secretary);
        review.setReviewStage(ReviewStage.SECRETARY);
        review.setReviewKind(kind);
        review.setDecision(ArticleReviewDecision.REQUEST_REVISION);
        review.setFeedback(feedback);
        review.setReviewedAt(now());                           // ck_review_decided: có quyết định thì có giờ
        reviews.save(review);
    }

    // Bài về lại tay Lead: REVISION_REQUIRED và xóa completed_at (ràng buộc ck_article_completed)
    private void returnArticleToLead(Article article) {
        article.setStatus(ArticleStatus.REVISION_REQUIRED);
        article.setCompletedAt(null);
        articles.save(article);
    }

    // Lý do không được để trống (chỉ toàn dấu cách cũng tính là trống)
    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) throw new ApplicationException(ErrorCode.PUBLICATION_REASON_REQUIRED);
        return reason.strip();
    }

    // ------------------------------------------------------------------ Kiểm tra dùng chung

    // Kiểm tra quyền ở backend: chỉ Thư ký tòa soạn đang ACTIVE (FR-12, R21)
    private Account requireSecretary(String email) {
        Account account = email == null ? null : accounts.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (account == null || account.getStatus() != AccountStatus.ACTIVE
                || account.getRole().getRoleName() != RoleName.EDITORIAL_SECRETARY)
            throw new ApplicationException(ErrorCode.PUBLICATION_ACCESS_DENIED);
        return account;
    }

    // Khóa dòng bài (SELECT ... FOR UPDATE) rồi mới kiểm tra trạng thái:
    // hai Thư ký bấm cùng lúc thì người sau thấy bài đã đổi trạng thái và bị chặn
    private Article lockApprovedArticle(Integer articleId) {
        Article article = articles.findByIdForUpdate(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        if (article.getStatus() != ArticleStatus.APPROVED)
            throw new ApplicationException(ErrorCode.PUBLICATION_NOT_APPROVED);
        return article;
    }

    // Bản được đăng = phiên bản mới nhất, và phiên bản đó phải có lượt Thư ký duyệt cuối (APPROVED)
    private ArticleVersion requireFinalApprovedVersion(Article article) {
        ArticleVersion latest = versions.findFirstByArticleOrderByVersionNumberDesc(article)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_SUBMITTED));
        if (!reviews.existsByVersionAndReviewStageAndDecision(latest, ReviewStage.SECRETARY, ArticleReviewDecision.APPROVED))
            throw new ApplicationException(ErrorCode.PUBLICATION_FINAL_APPROVAL_MISSING);
        return latest;
    }

    // Lượt duyệt APPROVED của một cấp trên đúng version này (null nếu không có)
    private ArticleReview findDecision(Article article, ArticleVersion version, ReviewStage stage) {
        return reviews.findByVersionArticleAndDecisionIsNotNullOrderByReviewedAtAscIdAsc(article).stream()
                .filter(r -> Objects.equals(r.getVersion().getId(), version.getId()))
                .filter(r -> r.getReviewStage() == stage && r.getDecision() == ArticleReviewDecision.APPROVED)
                .reduce((first, second) -> second)   // lấy lượt mới nhất
                .orElse(null);
    }

    // Ô <input type="datetime-local"> gửi lên dạng "2026-11-01T08:30" (không có múi giờ) -> hiểu là giờ Việt Nam
    private static OffsetDateTime parseScheduleTime(String text) {
        if (text == null || text.isBlank()) throw new ApplicationException(ErrorCode.PUBLICATION_SCHEDULE_TIME_REQUIRED);
        try {
            return LocalDateTime.parse(text.strip()).truncatedTo(ChronoUnit.MINUTES).atOffset(VN);
        } catch (DateTimeParseException ex) {
            throw new ApplicationException(ErrorCode.PUBLICATION_SCHEDULE_TIME_REQUIRED);
        }
    }

    // Các đoạn văn cách nhau bằng một dòng trống (quy ước của bảng article_version)
    private static List<String> splitParagraphs(String content) {
        if (content == null || content.isBlank()) return List.of();
        return Arrays.stream(content.strip().split("\\R\\s*\\R")).map(String::strip).filter(p -> !p.isEmpty()).toList();
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(VN).truncatedTo(ChronoUnit.SECONDS);
    }

    private static String statusLabel(ArticleStatus status) {
        return switch (status) {
            case APPROVED -> "Chờ xuất bản";
            case SCHEDULED -> "Đã hẹn giờ đăng";
            case PUBLISHED -> "Đã xuất bản";
            default -> status.name();
        };
    }
}
