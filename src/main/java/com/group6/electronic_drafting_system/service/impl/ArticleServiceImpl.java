package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Article.ArticleCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleDraftRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleEditorDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleListItemDTO;
import com.group6.electronic_drafting_system.dto.Article.AssignedTopicDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleSource;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.Article.ArticleMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleReviewRepository;
import com.group6.electronic_drafting_system.repository.ArticleSourceRepository;
import com.group6.electronic_drafting_system.repository.ArticleVersionRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.TopicRepository;
import com.group6.electronic_drafting_system.service.ArticleService;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleServiceImpl implements ArticleService {
    // Chỉ sửa được khi bài còn ở tay phóng viên
    private static final Set<ArticleStatus> EDITABLE = EnumSet.of(ArticleStatus.DRAFT, ArticleStatus.REVISION_REQUIRED);
    // Thẻ / thuộc tính trình soạn thảo được phép sinh ra; mọi thứ khác (script, on*, javascript:) bị loại bỏ
    private static final Safelist CONTENT_SAFELIST = Safelist.relaxed()
            .addTags("figure", "figcaption", "hr", "mark", "s", "span")
            .addAttributes(":all", "style")
            .addAttributes("blockquote", "class")
            .addAttributes("figure", "class")
            .addAttributes("img", "data-source-id")
            .addAttributes("a", "target", "rel")
            .preserveRelativeLinks(true);
    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings().prettyPrint(false);

    private final ArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final ArticleSourceRepository articleSources;
    private final ArticleReviewRepository reviews;
    private final AssignmentRepository assignments;
    private final TopicRepository topics;
    private final AccountRepository accounts;
    private final ArticleMapper articleMapper;

    public ArticleServiceImpl(ArticleRepository articles, ArticleVersionRepository versions, ArticleSourceRepository articleSources,
                              ArticleReviewRepository reviews, AssignmentRepository assignments, TopicRepository topics,
                              AccountRepository accounts, ArticleMapper articleMapper) {
        this.articles = articles;
        this.versions = versions;
        this.articleSources = articleSources;
        this.reviews = reviews;
        this.assignments = assignments;
        this.topics = topics;
        this.accounts = accounts;
        this.articleMapper = articleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleListItemDTO> getMyArticles(String email) {
        Account me = currentAccount(email);
        return articles.findByLead_IdOrderByUpdatedAtDesc(me.getId()).stream().map(articleMapper::toListItemDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedTopicDTO> getTopicsWaitingForArticle(String email) {
        Account me = currentAccount(email);
        return assignments.findByAccount_IdAndAssignmentRoleAndStatusAndTopic_Status(
                        me.getId(), AssignmentRole.LEAD, AssignmentStatus.ASSIGNED, TopicStatus.ACTIVE).stream()
                .filter(a -> !articles.existsByTopic_IdAndLead_Id(a.getTopic().getId(), me.getId()))
                .map(articleMapper::toAssignedTopicDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleEditorDTO getNewArticleForm(ArticleCreateRequestDTO form, String email) {
        Account me = currentAccount(email);
        // Đề tài không hợp lệ (không được giao / chưa ACTIVE) thì coi như chưa chọn
        Optional<Assignment> lead = Optional.ofNullable(form.getTopicId())
                .flatMap(topicId -> leadAssignment(topicId, me.getId()))
                .filter(a -> a.getTopic().getStatus() == TopicStatus.ACTIVE);
        Topic topic = lead.map(Assignment::getTopic).orElse(null);
        return articleMapper.toNewEditorDTO(form, topic, me,
                lead.map(Assignment::getDeadline).orElse(null),
                topic == null ? null : reviewerName(topic.getId()));
    }

    @Override
    @Transactional
    public Integer create(ArticleCreateRequestDTO request, boolean submit, String email) {
        Account me = currentAccount(email);
        if (request.getTopicId() == null) throw new ApplicationException(ErrorCode.ARTICLE_TOPIC_REQUIRED);
        Topic topic = topics.findById(request.getTopicId()).orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_TOPIC_NOT_ASSIGNED));
        if (leadAssignment(topic.getId(), me.getId()).isEmpty() || topic.getStatus() != TopicStatus.ACTIVE)
            throw new ApplicationException(ErrorCode.ARTICLE_TOPIC_NOT_ASSIGNED);
        if (articles.existsByTopic_IdAndLead_Id(topic.getId(), me.getId())) throw new ApplicationException(ErrorCode.ARTICLE_ALREADY_EXISTS);
        validateDraft(request);

        Article article = articleMapper.toEntity(request, topic, me);
        article.setContent(sanitize(article.getContent()));
        article = articles.save(article);
        if (submit) submitVersion(article, request.getChangeSummary());
        return article.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleEditorDTO getEditor(Integer articleId, String email) {
        Account me = currentAccount(email);
        Article article = articles.findWithDetailsById(articleId).orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        if (!article.getLead().getId().equals(me.getId())) throw new ApplicationException(ErrorCode.ARTICLE_FORBIDDEN);

        Integer topicId = article.getTopic().getId();
        return articleMapper.toEditorDTO(article, EDITABLE.contains(article.getStatus()),
                leadAssignment(topicId, me.getId()).map(Assignment::getDeadline).orElse(null),
                reviewerName(topicId),
                reviews.findFirstByVersion_Article_IdOrderByIdDesc(articleId).map(articleMapper::toReviewItem).orElse(null),
                versions.findByArticle_IdOrderByVersionNumberDesc(articleId).stream().map(articleMapper::toVersionItem).toList(),
                articleSources.findByArticleOrdered(articleId).stream().map(articleMapper::toSourceItem).toList());
    }

    @Override
    @Transactional
    public OffsetDateTime saveDraft(Integer articleId, ArticleDraftRequestDTO request, String email) {
        Article article = editableArticle(articleId, email);
        applyDraft(article, request);
        return article.getUpdatedAt();
    }

    @Override
    @Transactional
    public void submit(Integer articleId, ArticleDraftRequestDTO request, String email) {
        Article article = editableArticle(articleId, email);
        applyDraft(article, request);
        submitVersion(article, request.getChangeSummary());
    }

    @Override
    @Transactional
    public void setFeaturedImage(Integer articleId, Integer articleSourceId, String email) {
        editableArticle(articleId, email);
        List<ArticleSource> all = articleSources.findByArticleOrdered(articleId);
        ArticleSource chosen = all.stream().filter(s -> s.getId().equals(articleSourceId)).findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_SOURCE_NOT_FOUND));
        all.forEach(s -> s.setFeatured(s == chosen));
    }

    // Nộp bài: chụp article_version mới và chuyển sang chờ Trưởng ban duyệt
    private void submitVersion(Article article, String changeSummary) {
        if (article.getSapo() == null) throw new ApplicationException(ErrorCode.ARTICLE_SAPO_REQUIRED);
        if (Jsoup.parse(article.getContent()).text().isBlank()) throw new ApplicationException(ErrorCode.ARTICLE_CONTENT_REQUIRED);
        versions.save(articleMapper.toVersion(article, versions.findMaxVersionNumber(article.getId()) + 1, changeSummary));
        article.setStatus(ArticleStatus.DEPARTMENT_REVIEW);
        article.setSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private void applyDraft(Article article, ArticleDraftRequestDTO request) {
        validateDraft(request);
        articleMapper.updateEntity(request, article);
        article.setContent(sanitize(article.getContent()));
        // Article không có @PreUpdate nên tự cập nhật mốc thời gian
        article.setUpdatedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private static void validateDraft(ArticleDraftRequestDTO request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) throw new ApplicationException(ErrorCode.ARTICLE_TITLE_REQUIRED);
        if (request.getCategory() == null || request.getCategory().isBlank()) throw new ApplicationException(ErrorCode.ARTICLE_CATEGORY_REQUIRED);
    }

    private Article editableArticle(Integer articleId, String email) {
        Account me = currentAccount(email);
        Article article = articles.findById(articleId).orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        if (!article.getLead().getId().equals(me.getId())) throw new ApplicationException(ErrorCode.ARTICLE_FORBIDDEN);
        if (!EDITABLE.contains(article.getStatus())) throw new ApplicationException(ErrorCode.ARTICLE_NOT_EDITABLE);
        return article;
    }

    private Optional<Assignment> leadAssignment(Integer topicId, Integer accountId) {
        return assignments.findFirstByTopic_IdAndAccount_IdAndAssignmentRoleAndStatusNot(
                topicId, accountId, AssignmentRole.LEAD, AssignmentStatus.REMOVED);
    }

    private String reviewerName(Integer topicId) {
        return assignments.findFirstByTopic_IdAndAssignmentRoleAndStatusNot(topicId, AssignmentRole.DEPARTMENT_HEAD, AssignmentStatus.REMOVED)
                .map(a -> a.getAccount().getFullName()).orElse(null);
    }

    private Account currentAccount(String email) {
        if (email == null) throw new ApplicationException(ErrorCode.ARTICLE_FORBIDDEN);
        return accounts.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_FORBIDDEN));
    }

    private static String sanitize(String html) {
        if (html == null || html.isBlank()) return "";
        return Jsoup.clean(html, "http://localhost/", CONTENT_SAFELIST, OUTPUT);
    }
}
