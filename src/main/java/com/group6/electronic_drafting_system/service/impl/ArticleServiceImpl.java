package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Article.ArticleCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleDraftRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleEditorDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleListPageDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleMediaUploadResponseDTO;
import com.group6.electronic_drafting_system.dto.Article.AssignedTopicDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.ArticleSource;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Comment;
import com.group6.electronic_drafting_system.entity.Source;
import com.group6.electronic_drafting_system.entity.TopicSource;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.Article.ArticleMapper;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.ReviewWorkspaceMapper;
import com.group6.electronic_drafting_system.mapper.Reporter.ReporterTaskMapper;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleReviewRepository;
import com.group6.electronic_drafting_system.repository.ArticleSourceRepository;
import com.group6.electronic_drafting_system.repository.ArticleVersionRepository;
import com.group6.electronic_drafting_system.repository.AssignmentRepository;
import com.group6.electronic_drafting_system.repository.CommentRepository;
import com.group6.electronic_drafting_system.repository.SourceRepository;
import com.group6.electronic_drafting_system.repository.TopicSourceRepository;
import com.group6.electronic_drafting_system.service.ArticleService;
import com.group6.electronic_drafting_system.service.MediaStorageService;
import com.group6.electronic_drafting_system.service.MediaStorageService.MediaKind;
import com.group6.electronic_drafting_system.service.MediaStorageService.StoredMedia;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
            .addTags("video")
            .addAttributes("video", "src", "controls", "preload", "playsinline", "poster")
            .addProtocols("video", "src", "https")
            .addProtocols("video", "poster", "https")
            .preserveRelativeLinks(true);
    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings().prettyPrint(false);

    private final ArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final ArticleSourceRepository articleSources;
    private final ArticleReviewRepository reviews;
    private final CommentRepository comments;
    private final AssignmentRepository assignments;
    private final AccountRepository accounts;
    private final ArticleMapper articleMapper;
    private final SourceRepository sourceRepository;
    private final MediaStorageService mediaStorage;
    private final TopicSourceRepository topicSources;
    // Dùng chung cách chia nội dung thành từng đoạn với màn Trưởng ban (vị trí góp ý tính theo cách chia này)
    private final ReviewWorkspaceMapper versionViewMapper;

    public ArticleServiceImpl(ArticleRepository articles, ArticleVersionRepository versions, ArticleSourceRepository articleSources,
                              ArticleReviewRepository reviews, CommentRepository comments, AssignmentRepository assignments,
                              AccountRepository accounts, ArticleMapper articleMapper,
                              SourceRepository sourceRepository, MediaStorageService mediaStorage, TopicSourceRepository topicSources,
                              ReviewWorkspaceMapper versionViewMapper) {
        this.articles = articles;
        this.versions = versions;
        this.articleSources = articleSources;
        this.reviews = reviews;
        this.comments = comments;
        this.assignments = assignments;
        this.accounts = accounts;
        this.articleMapper = articleMapper;
        this.sourceRepository = sourceRepository;
        this.mediaStorage = mediaStorage;
        this.topicSources = topicSources;
        this.versionViewMapper = versionViewMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleListPageDTO getListPage(String email) {
        Account me = currentAccount(email);
        List<Article> mine = articles.findByLead_IdOrderByUpdatedAtDesc(me.getId());
        Map<Integer, Long> countByTopic = mine.stream().collect(Collectors.groupingBy(a -> a.getTopic().getId(), Collectors.counting()));
        // BR-01: một đề tài có nhiều bài, nên nhiệm vụ viết chính đã có bài vẫn được viết thêm
        List<AssignedTopicDTO> writable = assignments.findByAccount_IdAndAssignmentRoleAndStatusAndTopic_Status(
                        me.getId(), AssignmentRole.LEAD, AssignmentStatus.ASSIGNED, TopicStatus.ACTIVE).stream()
                .map(a -> articleMapper.toAssignedTopicDTO(a, countByTopic.getOrDefault(a.getTopic().getId(), 0L)))
                .toList();
        String department = me.getDepartment() == null ? null : me.getDepartment().getDepartmentName();
        return new ArticleListPageDTO(me.getFullName(), department, ReporterTaskMapper.initials(me.getFullName()),
                writable, mine.stream().map(articleMapper::toListItemDTO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleEditorDTO getNewArticleForm(ArticleCreateRequestDTO form, String email) {
        Account me = currentAccount(email);
        Assignment assignment = writableAssignment(form.getAssignmentId(), me);
        Integer topicId = assignment.getTopic().getId();
        return articleMapper.toNewEditorDTO(form, assignment, me, reviewerName(topicId), topicMedia(topicId));
    }

    @Override
    @Transactional
    public Integer create(ArticleCreateRequestDTO request, boolean submit, String email) {
        Account me = currentAccount(email);
        Assignment assignment = writableAssignment(request.getAssignmentId(), me);
        validateDraft(request);

        Article article = articleMapper.toEntity(request, assignment.getTopic(), me);
        article.setContent(sanitize(article.getContent()));
        article = articles.save(article);
        if (submit) submitVersion(article, request.getChangeSummary());
        return article.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleEditorDTO getEditor(Integer articleId, String email) {
        Account me = currentAccount(email);
        Article article = articles.findWithTopicAndLeadById(articleId).orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        if (!article.getLead().getId().equals(me.getId())) throw new ApplicationException(ErrorCode.ARTICLE_FORBIDDEN);

        Integer topicId = article.getTopic().getId();
        // Bị gỡ khỏi vai trò viết chính thì chỉ còn xem được bài cũ
        Assignment lead = leadAssignment(topicId, me.getId()).orElse(null);
        List<ArticleEditorDTO.Version> history = versionHistory(article);
        // Bài đang bị trả về: phiên bản bị trả về là phiên bản mới nhất (danh sách xếp mới nhất trước)
        ArticleEditorDTO.Version returnedVersion =
                article.getStatus() == ArticleStatus.REVISION_REQUIRED && !history.isEmpty() ? history.get(0) : null;
        return articleMapper.toEditorDTO(article, lead != null && EDITABLE.contains(article.getStatus()), lead,
                reviewerName(topicId),
                // Chỉ lấy phản hồi đã có quyết định: lượt đang duyệt dở thì phóng viên chưa được thấy
                reviews.findFirstByVersion_Article_IdAndDecisionIsNotNullOrderByIdDesc(articleId).map(articleMapper::toReviewItem).orElse(null),
                history,
                returnedVersion,
                articleSources.findByArticleOrdered(articleId).stream().map(articleMapper::toSourceItem).toList(),
                topicMedia(topicId));
    }

    // Ảnh / video trong kho tư liệu của đề tài để chèn vào bài
    private List<ArticleEditorDTO.TopicMedia> topicMedia(Integer topicId) {
        return topicSources.findByTopic_IdOrderByAttachedAtAsc(topicId).stream()
                .map(articleMapper::toTopicMedia).filter(Objects::nonNull).toList();
    }

    // Lịch sử phiên bản: mỗi bản kèm nhận xét của từng cấp duyệt và các góp ý (comment) gắn trên bản đó.
    //
    // Quy tắc phóng viên được xem gì (kiểm tra ở backend, không chỉ ẩn trên giao diện):
    //   1. Lượt duyệt chưa có quyết định (decision = NULL) là lượt Trưởng ban / Thư ký đang duyệt dở: KHÔNG trả về.
    //   2. Góp ý (comment) chỉ trả về khi phiên bản đó ĐÃ BỊ TRẢ VỀ (có lượt duyệt "Yêu cầu chỉnh sửa").
    //      Lúc đó trả cả góp ý của Trưởng ban lẫn Thư ký trên phiên bản đó, mỗi góp ý ghi rõ người góp ý và cấp.
    private List<ArticleEditorDTO.Version> versionHistory(Article article) {
        List<ArticleVersion> all = versions.findByArticle_IdOrderByVersionNumberDesc(article.getId());
        if (all.isEmpty()) return List.of();

        // Quy tắc 1: chỉ giữ lượt duyệt đã có quyết định, xếp theo thứ tự thời gian (Trưởng ban trước, Thư ký sau)
        List<ArticleReview> decidedReviews = reviews.findByVersionIn(all).stream()
                .filter(r -> r.getDecision() != null)
                .sorted(Comparator.comparing(ArticleReview::getId))
                .toList();
        Map<Integer, List<ArticleReview>> reviewsByVersion = decidedReviews.stream()
                .collect(Collectors.groupingBy(r -> r.getVersion().getId()));

        // Quy tắc 2: các phiên bản đã bị trả về (có lượt duyệt REQUEST_REVISION)
        Set<Integer> returnedVersionIds = decidedReviews.stream()
                .filter(r -> r.getDecision() == ArticleReviewDecision.REQUEST_REVISION)
                .map(r -> r.getVersion().getId())
                .collect(Collectors.toSet());

        // Chỉ giữ góp ý thuộc lượt duyệt đã có quyết định, trên phiên bản đã bị trả về
        List<Comment> visibleComments = comments.findByReviewVersionArticleOrderByCreatedAtAscIdAsc(article).stream()
                .filter(c -> c.getReview().getDecision() != null)
                .filter(c -> returnedVersionIds.contains(c.getReview().getVersion().getId()))
                .toList();
        Map<Integer, List<Comment>> repliesByParent = visibleComments.stream().filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));
        Map<Integer, List<ArticleEditorDTO.VersionComment>> commentsByVersion = visibleComments.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.groupingBy(c -> c.getReview().getVersion().getId(), Collectors.mapping(
                        c -> articleMapper.toVersionComment(c, repliesByParent.getOrDefault(c.getId(), List.of()).stream()
                                .map(r -> articleMapper.toVersionComment(r, List.of())).toList()),
                        Collectors.toList())));

        return all.stream()
                .map(v -> articleMapper.toVersionItem(v,
                        reviewsByVersion.getOrDefault(v.getId(), List.of()).stream().map(articleMapper::toVersionReview).toList(),
                        commentsByVersion.getOrDefault(v.getId(), List.of()),
                        // Chia đoạn bằng đúng mapper của màn Trưởng ban để vị trí góp ý khớp tuyệt đối
                        versionViewMapper.toVersionView(v)))
                .toList();
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

    // Lấy ảnh / video từ tư liệu đề tài vào bài: gắn source đó vào article_source (nếu chưa gắn) để dùng làm tư liệu / ảnh đại diện
    @Override
    @Transactional
    public ArticleMediaUploadResponseDTO attachTopicMedia(Integer articleId, Integer sourceId, String email) {
        Article article = editableArticle(articleId, email);
        TopicSource topicSource = topicSources.findByTopic_IdAndSource_Id(article.getTopic().getId(), sourceId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_SOURCE_NOT_FOUND));
        ArticleEditorDTO.TopicMedia media = articleMapper.toTopicMedia(topicSource);
        if (media == null) throw new ApplicationException(ErrorCode.TOPIC_SOURCE_NOT_FOUND);
        MediaKind kind = media.video() ? MediaKind.VIDEO : MediaKind.IMAGE;
        String caption = trimTo(media.note(), 300);

        Optional<ArticleSource> existing = articleSources.findByArticle_IdAndSource_Id(articleId, sourceId);
        if (existing.isPresent()) {
            ArticleSource attached = existing.get();
            return new ArticleMediaUploadResponseDTO(true, "Đã chèn tư liệu đề tài", kind, media.fileUrl(), attached.getId(), media.title(),
                    attached.getCaption() != null ? attached.getCaption() : caption, false);
        }
        // Bài chưa có ảnh đại diện thì lấy luôn ảnh vừa chọn
        boolean featured = kind == MediaKind.IMAGE
                && articleSources.findByArticleOrdered(articleId).stream().noneMatch(s -> Boolean.TRUE.equals(s.getFeatured()));
        ArticleSource attached = new ArticleSource();
        attached.setArticle(article);
        attached.setSource(topicSource.getSource());
        attached.setCaption(caption);
        attached.setNote("Lấy từ tư liệu đề tài");
        attached.setFeatured(featured);
        articleSources.save(attached);
        return new ArticleMediaUploadResponseDTO(true, "Đã chèn tư liệu đề tài", kind, media.fileUrl(), attached.getId(), media.title(), caption, featured);
    }

    // Tải ảnh / video từ máy lên Cloudinary; DB chỉ lưu đường dẫn (secure_url).
    // Ảnh: tạo source IMAGE + gắn article_source để dùng làm tư liệu / ảnh đại diện.
    // Video: source_type trong DB chưa có VIDEO nên chỉ trả url để chèn vào nội dung bài.
    @Override
    @Transactional
    public ArticleMediaUploadResponseDTO uploadMedia(Integer articleId, MultipartFile file, String caption, String email) {
        Article article = editableArticle(articleId, email);
        String note = trimTo(caption, 300);
        StoredMedia stored = mediaStorage.upload(file, "article-" + articleId);
        if (stored.kind() == MediaKind.VIDEO)
            return new ArticleMediaUploadResponseDTO(true, "Đã tải video lên", MediaKind.VIDEO, stored.url(), null, null, note, false);
        try {
            Source source = new Source();
            source.setAddedBy(article.getLead());
            source.setTitle(note != null ? trimTo(note, 255) : fileTitle(file.getOriginalFilename()));
            source.setSourceType(SourceType.IMAGE);
            source.setFileUrl(stored.url());
            sourceRepository.save(source);

            // Bài chưa có ảnh đại diện thì lấy luôn ảnh vừa tải
            boolean featured = articleSources.findByArticleOrdered(articleId).stream().noneMatch(s -> Boolean.TRUE.equals(s.getFeatured()));
            ArticleSource attached = new ArticleSource();
            attached.setArticle(article);
            attached.setSource(source);
            attached.setCaption(note);
            attached.setFeatured(featured);
            articleSources.save(attached);
            return new ArticleMediaUploadResponseDTO(true, "Đã tải ảnh lên", MediaKind.IMAGE, stored.url(),
                    attached.getId(), source.getTitle(), note, featured);
        } catch (RuntimeException ex) {
            mediaStorage.delete(stored);
            throw ex;
        }
    }

    private static String fileTitle(String filename) {
        String name = filename == null || filename.isBlank() ? "Ảnh tải lên" : filename.trim();
        return trimTo(name, 255);
    }

    private static String trimTo(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    // Nộp bài: chụp article_version mới và chuyển sang chờ Trưởng ban duyệt
    private void submitVersion(Article article, String changeSummary) {
        if (article.getSapo() == null) throw new ApplicationException(ErrorCode.ARTICLE_SAPO_REQUIRED);
        if (Jsoup.parse(article.getContent()).text().isBlank()) throw new ApplicationException(ErrorCode.ARTICLE_CONTENT_REQUIRED);
        versions.save(articleMapper.toVersion(article, versions.findFirstByArticleOrderByVersionNumberDesc(article).map(ArticleVersion::getVersionNumber).orElse(0) + 1, changeSummary));
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
        if (leadAssignment(article.getTopic().getId(), me.getId()).isEmpty()) throw new ApplicationException(ErrorCode.ARTICLE_ASSIGNMENT_NOT_LEAD);
        if (!EDITABLE.contains(article.getStatus())) throw new ApplicationException(ErrorCode.ARTICLE_NOT_EDITABLE);
        return article;
    }

    // Chỉ viết bài mới từ nhiệm vụ giao cho chính mình, vai trò viết chính, còn hiệu lực, trên đề tài đã duyệt
    private Assignment writableAssignment(Integer assignmentId, Account me) {
        if (assignmentId == null) throw new ApplicationException(ErrorCode.ARTICLE_ASSIGNMENT_REQUIRED);
        return assignments.findWithDetailsById(assignmentId)
                .filter(a -> a.getAccount().getId().equals(me.getId()))
                .filter(a -> a.getAssignmentRole() == AssignmentRole.LEAD && a.getStatus() == AssignmentStatus.ASSIGNED)
                .filter(a -> a.getTopic().getStatus() == TopicStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_ASSIGNMENT_NOT_LEAD));
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
