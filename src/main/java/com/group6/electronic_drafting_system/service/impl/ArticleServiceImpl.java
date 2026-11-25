package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Article.ArticleCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleCreatedWithMediaDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleDraftRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleEditorDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleListItemDTO;
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
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.TopicSource;
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.Article.ArticleMapper;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.ReviewWorkspaceMapper;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
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
    // Thẻ / thuộc tính trình soạn thảo được phép sinh ra; mọi thứ khác (script,
    // on*, javascript:) bị loại bỏ
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
    // Dùng chung cách chia nội dung thành từng đoạn với màn Trưởng ban (vị trí góp
    // ý tính theo cách chia này)
    private final ReviewWorkspaceMapper versionViewMapper;

    // Spring tự truyền các repository / mapper / service cần dùng vào đây (constructor injection)
    public ArticleServiceImpl(ArticleRepository articles, ArticleVersionRepository versions,
            ArticleSourceRepository articleSources,
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

    // Trang "Bài viết của tôi": các bài mình là Lead + các nhiệm vụ viết chính còn được viết bài mới.
    @Override
    @Transactional(readOnly = true)
    public ArticleListPageDTO getListPage(String email) {
        Account me = currentAccount(email);
        List<Article> mine = articles.findByLead_IdOrderByUpdatedAtDesc(me.getId());
        Map<Integer, Long> countByTopic = mine.stream()
                .collect(Collectors.groupingBy(a -> a.getTopic().getId(), Collectors.counting()));
        // BR-01: một đề tài có nhiều bài, nên nhiệm vụ viết chính đã có bài vẫn được
        // viết thêm
        List<AssignedTopicDTO> writable = assignments.findByAccount_IdAndAssignmentRoleAndStatusAndTopic_Status(
                me.getId(), AssignmentRole.LEAD, AssignmentStatus.ASSIGNED, TopicStatus.ACTIVE).stream()
                .map(a -> articleMapper.toAssignedTopicDTO(a, countByTopic.getOrDefault(a.getTopic().getId(), 0L)))
                .toList();
        List<ArticleListItemDTO> items = mine.stream()
                .map(a -> articleMapper.toListItemDTO(a, statusLabel(a.getStatus()), statusGroup(a.getStatus())))
                .toList();
        return new ArticleListPageDTO(me.getFullName(), departmentName(me), initials(me.getFullName()),
                writable, items);
    }

    // Trang "Viết bài mới" từ một nhiệm vụ viết chính. form: dữ liệu đã nhập (khi nộp lỗi phải hiện lại), có thể trống.
    @Override
    @Transactional(readOnly = true)
    public ArticleEditorDTO getNewArticleForm(ArticleCreateRequestDTO form, String email) {
        Account me = currentAccount(email);
        Assignment assignment = writableAssignment(form.getAssignmentId(), me);   // Kiểm tra quyền: phải là Lead của nhiệm vụ
        Topic topic = assignment.getTopic();
        // Chưa nhập gì thì lấy sẵn tít và chuyên mục theo đề tài
        String title = form.getTitle() != null ? form.getTitle() : topic.getTitle();
        String category = form.getCategory() != null ? form.getCategory() : topic.getCategory();
        String content = form.getContent() == null ? "" : form.getContent();
        return articleMapper.toNewEditorDTO(form, assignment, me, title, category, content,
                statusLabel(ArticleStatus.DRAFT), priorityLabel(topic.getPriority()), initials(me.getFullName()),
                departmentName(me), reviewerName(topic.getId()), topicMedia(topic.getId()));
    }

    // Tạo bài mới (DRAFT) từ nhiệm vụ viết chính; submit = true thì nộp luôn (tạo version 1.0).
    @Override
    @Transactional
    public Integer create(ArticleCreateRequestDTO request, boolean submit, String email) {
        Account me = currentAccount(email);
        Assignment assignment = writableAssignment(request.getAssignmentId(), me);   // Kiểm tra quyền: phải là Lead của nhiệm vụ
        validateDraft(request);
        normalizeDraft(request);

        Article article = articleMapper.toEntity(request, assignment.getTopic(), me);
        article.setContent(sanitize(article.getContent()));
        article = articles.save(article);
        if (submit)
            submitVersion(article, request.getChangeSummary());
        return article.getId();
    }

    // Trang soạn / xem một bài của chính mình: nội dung, lịch sử phiên bản, góp ý, tư liệu và ảnh / video của đề tài.
    @Override
    @Transactional(readOnly = true)
    public ArticleEditorDTO getEditor(Integer articleId, String email) {
        Account me = currentAccount(email);
        Article article = articles.findWithTopicAndLeadById(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        if (!article.getLead().getId().equals(me.getId()))
            throw new ApplicationException(ErrorCode.ARTICLE_FORBIDDEN);

        Integer topicId = article.getTopic().getId();
        // Bị gỡ khỏi vai trò viết chính thì chỉ còn xem được bài cũ
        Assignment lead = leadAssignment(topicId, me.getId()).orElse(null);
        List<ArticleEditorDTO.Version> history = versionHistory(article);
        // Bài đang bị trả về: phiên bản bị trả về là phiên bản mới nhất (danh sách xếp
        // mới nhất trước)
        ArticleEditorDTO.Version returnedVersion = article.getStatus() == ArticleStatus.REVISION_REQUIRED
                && !history.isEmpty() ? history.get(0) : null;
        // Vừa khôi phục version nào (restored_from_version_id) -> lấy nhãn "1.0",
        // "1.1"... để điền sẵn ghi chú
        String restoredFromLabel = history.stream()
                .filter(v -> v.id().equals(article.getRestoredFromVersionId()))
                .map(ArticleEditorDTO.Version::label)
                .findFirst().orElse(null);
        // Chỉ lấy phản hồi đã có quyết định: lượt đang duyệt dở thì phóng viên chưa
        // được thấy
        ArticleEditorDTO.Review latestReview = reviews.findFirstByVersion_Article_IdAndDecisionIsNotNullOrderByIdDesc(articleId)
                .map(r -> articleMapper.toReviewItem(r, stageLabel(r.getReviewStage()))).orElse(null);

        // Tư liệu của bài; tách riêng các ảnh và ảnh đại diện để trang soạn bài dùng
        List<ArticleEditorDTO.Source> sources = articleSources.findByArticleOrdered(articleId).stream()
                .map(articleMapper::toSourceItem).toList();
        List<ArticleEditorDTO.Source> imageSources = sources.stream().filter(ArticleServiceImpl::isImageSource).toList();
        ArticleEditorDTO.Source featuredImage = imageSources.stream().filter(ArticleEditorDTO.Source::featured)
                .findFirst().orElse(null);

        Account leadAccount = article.getLead();
        return articleMapper.toEditorDTO(article, lead != null && EDITABLE.contains(article.getStatus()), lead,
                statusLabel(article.getStatus()), priorityLabel(article.getTopic().getPriority()),
                initials(leadAccount.getFullName()), departmentName(leadAccount), reviewerName(topicId),
                latestReview,
                history,
                returnedVersion,
                restoredFromLabel,
                sources, featuredImage, imageSources,
                topicMedia(topicId));
    }

    // Khôi phục version cũ (UC-24, EC-03, BR-18): chép tít, tít phụ, sapo, nội dung
    // của version vào BẢN ĐANG SOẠN.
    // - Không sửa, không xóa, không tạo version nào (version chỉ tạo ở lần nộp lại
    // kế tiếp).
    // - Trả về nhãn version (vd "1.0") để controller báo cho người dùng.
    @Override
    @Transactional
    public String restoreVersion(Integer articleId, Integer versionId, String email) {
        // Kiểm tra quyền: phải là Lead đang hoạt động của bài và bài đang sửa được
        // (DRAFT / REVISION_REQUIRED)
        Article article = editableArticle(articleId, email);
        // Version phải thuộc đúng bài này (rule R22): tìm trong danh sách version của
        // bài, không tin id gửi lên
        ArticleVersion version = null;
        List<ArticleVersion> versionList = versions.findByArticle_IdOrderByVersionNumberDesc(articleId);

        for (ArticleVersion currentVersion : versionList) { // đi qua từng version
            if (currentVersion.getId().equals(versionId)) { // id trùng không?
                version = currentVersion; // trùng thì lấy
                break; // lấy được rồi thì dừng
            }
        }
        if (version == null) { // không tìm thấy thì báo lỗi
            throw new ApplicationException(ErrorCode.ARTICLE_VERSION_NOT_FOUND);
        }

        // Chép nội dung version vào bản đang soạn (chuyên mục và tư liệu đính kèm giữ
        // nguyên vì không lưu theo version)
        article.setTitle(version.getTitle());
        article.setSubtitle(version.getSubtitle());
        article.setSapo(version.getSapo());
        article.setContent(version.getContent());
        // Ghi lại đã khôi phục từ version nào (DB cho phép vì bài đang DRAFT /
        // REVISION_REQUIRED - ck_article_restore)
        article.setRestoredFromVersionId(version.getId());
        article.setUpdatedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return version.getVersionLabel();
    }

    // Ảnh / video trong kho tư liệu của đề tài để chèn vào bài (tư liệu khác bị bỏ qua)
    private List<ArticleEditorDTO.TopicMedia> topicMedia(Integer topicId) {
        return topicSources.findByTopic_IdOrderByAttachedAtAsc(topicId).stream()
                .map(this::toTopicMedia).filter(Objects::nonNull).toList();
    }

    // Một tư liệu đề tài -> TopicMedia. Trả null nếu tư liệu không phải ảnh / video.
    // Video được lưu với source_type khác IMAGE, nhận ra qua đường dẫn Cloudinary.
    private ArticleEditorDTO.TopicMedia toTopicMedia(TopicSource topicSource) {
        Source source = topicSource.getSource();
        boolean video = source.getSourceType() != SourceType.IMAGE
                && MediaStorageService.isVideoUrl(source.getFileUrl());
        boolean image = source.getSourceType() == SourceType.IMAGE && source.getFileUrl() != null
                && !source.getFileUrl().isBlank();
        if (!image && !video)
            return null;
        String note = trimToNull(topicSource.getNote());
        String addedByName = source.getAddedBy() == null ? null : source.getAddedBy().getFullName();
        // Chú thích hiển thị: ghi chú nếu có, không thì tên tư liệu
        String caption = note != null ? note : source.getTitle();
        return articleMapper.toTopicMedia(source, video, note, addedByName, caption);
    }

    // =====================================================================================================
    // LỊCH SỬ PHIÊN BẢN cho phóng viên (Lead). Gọi từ getEditor() khi mở trang soạn bài.
    // Trả về danh sách phiên bản (MỚI NHẤT TRƯỚC); mỗi phiên bản kèm:
    //   - các lượt duyệt ĐÃ CHỐT trên phiên bản đó (ai duyệt, cấp nào, kết quả, nhận xét)
    //   - các góp ý trên phiên bản đó
    //
    // QUY TẮC PHÓNG VIÊN ĐƯỢC XEM GÌ (kiểm tra ở backend, không chỉ ẩn trên giao diện):
    //   1. Lượt duyệt đang dở (decision = NULL) -> KHÔNG trả về (Trưởng phòng / Thư ký chưa quyết định).
    //   2. Góp ý chỉ trả về khi phiên bản đó ĐÃ BỊ TRẢ VỀ (có lượt duyệt "Yêu cầu sửa").
    //      Khi đó trả góp ý của cả Trưởng phòng lẫn Thư ký trên phiên bản đó.
    //
    // VÍ DỤ bài #11: v1.1 (Trưởng phòng đang duyệt dở), v1.0 (Trưởng phòng đã "Yêu cầu sửa", có 3 góp ý)
    //   -> v1.1: 0 lượt duyệt, 0 góp ý      v1.0: 1 lượt duyệt, 3 góp ý
    // =====================================================================================================
    private List<ArticleEditorDTO.Version> versionHistory(Article article) {
        // ===== BƯỚC 1: lấy mọi phiên bản của bài, mới nhất trước [v1.1, v1.0] =====
        List<ArticleVersion> allVersions = versions.findByArticle_IdOrderByVersionNumberDesc(article.getId());
        if (allVersions.isEmpty()) {
            return List.of();   // bài chưa nộp lần nào (DRAFT) -> chưa có lịch sử
        }

        // ===== BƯỚC 2: hỏi DB một lần, lấy hết dữ liệu cần dùng =====
        // Mọi lượt duyệt ĐÃ CHỐT (decision khác NULL) của bài, cũ trước mới sau (Trưởng phòng trước, Thư ký sau).
        // Lượt đang duyệt dở đã bị loại ngay trong câu SQL -> đúng quy tắc 1.
        List<ArticleReview> decidedReviews = reviews.findByVersionArticleAndDecisionIsNotNullOrderByReviewedAtAscIdAsc(article);
        // Mọi góp ý trên mọi phiên bản của bài, cũ trước mới sau
        List<Comment> allComments = comments.findByReviewVersionArticleOrderByCreatedAtAscIdAsc(article);

        // ===== BƯỚC 3: đi qua từng phiên bản, gom lượt duyệt và góp ý của RIÊNG phiên bản đó =====
        List<ArticleEditorDTO.Version> result = new ArrayList<>();
        for (ArticleVersion version : allVersions) {

            // 3a. Lượt duyệt của phiên bản này + xem phiên bản này có từng bị "Yêu cầu sửa" không
            List<ArticleEditorDTO.VersionReview> versionReviews = new ArrayList<>();
            boolean wasReturned = false;
            for (ArticleReview review : decidedReviews) {
                // Lượt duyệt thuộc phiên bản khác -> bỏ qua
                if (!review.getVersion().getId().equals(version.getId())) {
                    continue;
                }
                // Service tính nhãn, mapper chỉ đóng gói thành DTO
                versionReviews.add(articleMapper.toVersionReview(review, stageLabel(review.getReviewStage()),
                        decisionLabel(review.getDecision())));
                if (review.getDecision() == ArticleReviewDecision.REQUEST_REVISION) {
                    wasReturned = true;   // phiên bản này đã bị trả về
                }
            }

            // 3b. Góp ý của phiên bản này: CHỈ lấy khi phiên bản đã bị trả về (quy tắc 2)
            List<ArticleEditorDTO.VersionComment> versionComments = new ArrayList<>();
            if (wasReturned) {
                for (Comment comment : allComments) {
                    boolean sameVersion = comment.getReview().getVersion().getId().equals(version.getId());
                    boolean reviewDecided = comment.getReview().getDecision() != null;   // không lấy góp ý của lượt đang duyệt dở
                    boolean rootComment = comment.getParent() == null;                  // hệ thống không có chức năng trả lời góp ý
                    if (sameVersion && reviewDecided && rootComment) {
                        // List.of() = danh sách câu trả lời rỗng
                        versionComments.add(toVersionComment(comment, List.of()));
                    }
                }
            }

            // 3c. Đóng gói phiên bản: nội dung + lượt duyệt + góp ý.
            // toVersionView: chia đoạn bằng ĐÚNG mapper của màn Trưởng phòng, để tô vàng đúng vị trí góp ý.
            result.add(articleMapper.toVersionItem(version, versionLabel(version), versionReviews, versionComments,
                    versionViewMapper.toVersionView(version)));
        }
        return result;
    }

    // Lưu nháp: chỉ cập nhật bản đang soạn trên bảng article, KHÔNG tạo version (CLAUDE.md mục 7).
    // Trả về thời điểm lưu để giao diện hiện "Đã lưu lúc ...".
    @Override
    @Transactional
    public OffsetDateTime saveDraft(Integer articleId, ArticleDraftRequestDTO request, String email) {
        Article article = editableArticle(articleId, email);
        applyDraft(article, request);
        return article.getUpdatedAt();
    }

    // Nộp / nộp lại bài đang soạn: lưu nháp rồi tạo version mới, chuyển sang DEPARTMENT_REVIEW.
    // noRollbackFor: nếu bị lỗi kiểm tra (thiếu ghi chú, thiếu tư liệu...) thì VẪN
    // GIỮ bản nháp vừa sửa,
    // chỉ không tạo version. Không có dòng này, transaction bị hủy và Lead mất phần
    // vừa gõ.
    // An toàn vì submitVersion() kiểm tra hết TRƯỚC khi lưu version và đổi trạng
    // thái.
    @Override
    @Transactional(noRollbackFor = ApplicationException.class)
    public void submit(Integer articleId, ArticleDraftRequestDTO request, String email) {
        Article article = editableArticle(articleId, email);
        applyDraft(article, request);
        submitVersion(article, request.getChangeSummary());
    }

    // Chọn ảnh đại diện: bật featured cho tư liệu được chọn, tắt cho mọi tư liệu khác (mỗi bài tối đa một ảnh đại diện)
    @Override
    @Transactional
    public void setFeaturedImage(Integer articleId, Integer articleSourceId, String email) {
        editableArticle(articleId, email);
        List<ArticleSource> all = articleSources.findByArticleOrdered(articleId);
        ArticleSource chosen = all.stream().filter(s -> s.getId().equals(articleSourceId)).findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_SOURCE_NOT_FOUND));
        // Ảnh đại diện phải là tư liệu loại IMAGE (R16)
        if (chosen.getSource().getSourceType() != SourceType.IMAGE)
            throw new ApplicationException(ErrorCode.ARTICLE_SOURCE_NOT_FOUND);
        // Tắt ảnh cũ và ghi xuống DB trước (flush), rồi mới bật ảnh mới: nếu hai ảnh cùng bật
        // dù chỉ trong chốc lát thì vi phạm unique index uq_article_featured
        all.forEach(s -> s.setFeatured(false));
        articleSources.flush();
        chosen.setFeatured(true);
    }

    // Lấy ảnh / video từ tư liệu đề tài vào bài: gắn source đó vào article_source
    // (nếu chưa gắn) để dùng làm tư liệu / ảnh đại diện
    @Override
    @Transactional
    public ArticleMediaUploadResponseDTO attachTopicMedia(Integer articleId, Integer sourceId, String email) {
        Article article = editableArticle(articleId, email);
        TopicSource topicSource = topicSources.findByTopic_IdAndSource_Id(article.getTopic().getId(), sourceId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.TOPIC_SOURCE_NOT_FOUND));
        ArticleEditorDTO.TopicMedia media = toTopicMedia(topicSource);
        if (media == null)
            throw new ApplicationException(ErrorCode.TOPIC_SOURCE_NOT_FOUND);
        MediaKind kind = media.video() ? MediaKind.VIDEO : MediaKind.IMAGE;
        String caption = trimTo(media.note(), 300);

        Optional<ArticleSource> existing = articleSources.findByArticle_IdAndSource_Id(articleId, sourceId);
        if (existing.isPresent()) {
            ArticleSource attached = existing.get();
            return new ArticleMediaUploadResponseDTO(true, "Đã chèn tư liệu đề tài", kind, media.fileUrl(),
                    attached.getId(), media.title(),
                    attached.getCaption() != null ? attached.getCaption() : caption, false);
        }
        // Bài chưa có ảnh đại diện thì lấy luôn ảnh vừa chọn
        boolean featured = kind == MediaKind.IMAGE
                && articleSources.findByArticleOrdered(articleId).stream()
                        .noneMatch(s -> Boolean.TRUE.equals(s.getFeatured()));
        ArticleSource attached = new ArticleSource();
        attached.setArticle(article);
        attached.setSource(topicSource.getSource());
        attached.setCaption(caption);
        attached.setNote("Lấy từ tư liệu đề tài");
        attached.setFeatured(featured);
        articleSources.save(attached);
        return new ArticleMediaUploadResponseDTO(true, "Đã chèn tư liệu đề tài", kind, media.fileUrl(),
                attached.getId(), media.title(), caption, featured);
    }

    // Tải ảnh / video từ máy lên Cloudinary; DB chỉ lưu đường dẫn (secure_url).
    // Ảnh: tạo source IMAGE + gắn article_source để dùng làm tư liệu / ảnh đại
    // diện.
    // Video: source_type trong DB chưa có VIDEO nên chỉ trả url để chèn vào nội
    // dung bài.
    @Override
    @Transactional
    public ArticleMediaUploadResponseDTO uploadMedia(Integer articleId, MultipartFile file, String caption,
            String email) {
        Article article = editableArticle(articleId, email);
        String note = trimTo(caption, 300);
        StoredMedia stored = mediaStorage.upload(file, "article-" + articleId);
        if (stored.kind() == MediaKind.VIDEO)
            return new ArticleMediaUploadResponseDTO(true, "Đã tải video lên", MediaKind.VIDEO, stored.url(), null,
                    null, note, false);
        try {
            Source source = new Source();
            source.setAddedBy(article.getLead());
            source.setTitle(note != null ? trimTo(note, 255) : fileTitle(file.getOriginalFilename()));
            source.setSourceType(SourceType.IMAGE);
            source.setFileUrl(stored.url());
            sourceRepository.save(source);

            // Bài chưa có ảnh đại diện thì lấy luôn ảnh vừa tải
            boolean featured = articleSources.findByArticleOrdered(articleId).stream()
                    .noneMatch(s -> Boolean.TRUE.equals(s.getFeatured()));
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

    // Bài mới chưa lưu mà Lead tải ảnh / video: tự tạo bản nháp từ nội dung đang soạn, rồi tải tệp và gắn vào bài đó.
    // Cả hai bước nằm trong MỘT transaction: tải tệp lỗi (sai loại, quá lớn, Cloudinary lỗi...) thì bài nháp
    // cũng không được tạo, không để lại bài rác. Quyền được kiểm tra bên trong create() (phải là Lead của nhiệm vụ)
    // và uploadMedia() (phải là Lead của bài, bài đang sửa được).
    @Override
    @Transactional
    public ArticleCreatedWithMediaDTO createDraftWithMedia(ArticleCreateRequestDTO request, MultipartFile file,
            String caption, String email) {
        // 1. Tạo bài DRAFT (giống bấm "Tạo bản nháp"): chưa tạo version
        Integer articleId = create(request, false, email);
        // 2. Tải tệp lên và gắn vào bài vừa tạo (giống tải ảnh ở bài đã lưu)
        ArticleMediaUploadResponseDTO media = uploadMedia(articleId, file, caption, email);
        return new ArticleCreatedWithMediaDTO(true, "Đã tạo bản nháp. " + media.message(), articleId, media);
    }

    // Tên tư liệu khi người dùng không nhập chú thích: lấy tên tệp (tối đa 255 ký tự)
    private static String fileTitle(String filename) {
        String name = filename == null || filename.isBlank() ? "Ảnh tải lên" : filename.trim();
        return trimTo(name, 255);
    }

    // Bỏ khoảng trắng hai đầu và cắt còn tối đa max ký tự; chuỗi rỗng thì trả null
    private static String trimTo(String value, int max) {
        if (value == null || value.isBlank())
            return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    // Nộp bài: chụp article_version mới và chuyển sang chờ Trưởng ban duyệt
    // Dùng cho cả nộp lần đầu (FL-04) và nộp lại (FL-05). Kiểm tra HẾT rồi mới lưu
    // version và đổi trạng thái.
    private void submitVersion(Article article, String changeSummary) {
        // 1. Trường bắt buộc (FR-07)
        if (article.getSapo() == null)
            throw new ApplicationException(ErrorCode.ARTICLE_SAPO_REQUIRED);
        if (Jsoup.parse(article.getContent()).text().isBlank())
            throw new ApplicationException(ErrorCode.ARTICLE_CONTENT_REQUIRED);

        // 2. Ít nhất một tư liệu, và mọi ảnh có chú thích (FR-07, giống rule R19 trong
        // DB)
        // Bài vừa tạo (chưa lưu) thì chưa có id nên cũng chưa có tư liệu nào.
        List<ArticleSource> attached = article.getId() == null ? List.of()
                : articleSources.findByArticleOrdered(article.getId());
        if (attached.isEmpty())
            throw new ApplicationException(ErrorCode.ARTICLE_SOURCE_REQUIRED);
        boolean imageWithoutCaption = attached.stream()
                .anyMatch(s -> s.getSource().getSourceType() == SourceType.IMAGE
                        && (s.getCaption() == null || s.getCaption().isBlank()));
        if (imageWithoutCaption)
            throw new ApplicationException(ErrorCode.ARTICLE_IMAGE_CAPTION_REQUIRED);

        // 3. Nộp lại (đã có version trước): bắt buộc ghi chú và nội dung phải khác bản
        // trước (UC-25)
        Optional<ArticleVersion> latest = versions.findFirstByArticleOrderByVersionNumberDesc(article);
        if (latest.isPresent()) {
            if (changeSummary == null || changeSummary.isBlank()) {
                throw new ApplicationException(ErrorCode.ARTICLE_CHANGE_SUMMARY_REQUIRED);
            }
            if (sameContent(article, latest.get()))
                throw new ApplicationException(ErrorCode.ARTICLE_CONTENT_UNCHANGED);
        }

        // 4. Hợp lệ: chụp version mới (1.0, 1.1...) và chuyển sang chờ Trưởng ban duyệt
        int nextNumber = latest.map(ArticleVersion::getVersionNumber).orElse(0) + 1;
        versions.save(articleMapper.toVersion(article, nextNumber, trimToNull(changeSummary)));
        article.setStatus(ArticleStatus.DEPARTMENT_REVIEW);
        article.setSubmittedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        // Đã nộp thì xóa dấu "vừa khôi phục": DB chỉ cho có giá trị này khi bài DRAFT /
        // REVISION_REQUIRED (ck_article_restore)
        article.setRestoredFromVersionId(null);
    }

    // So bản đang soạn với version đã nộp: tít, tít phụ, sapo, nội dung (ảnh / tư
    // liệu đính kèm không tính)
    private static boolean sameContent(Article article, ArticleVersion version) {
        return sameText(article.getTitle(), version.getTitle())
                && sameText(article.getSubtitle(), version.getSubtitle())
                && sameText(article.getSapo(), version.getSapo())
                && sameText(article.getContent(), version.getContent());
    }

    // null và chuỗi rỗng coi như nhau; bỏ khoảng trắng thừa ở hai đầu
    private static boolean sameText(String a, String b) {
        String left = a == null ? "" : a.trim();
        String right = b == null ? "" : b.trim();
        return left.equals(right);
    }

    // Chép dữ liệu trình soạn thảo vào bản đang soạn: kiểm tra, chuẩn hóa, lọc HTML, cập nhật mốc thời gian
    private void applyDraft(Article article, ArticleDraftRequestDTO request) {
        validateDraft(request);
        normalizeDraft(request);
        articleMapper.updateEntity(request, article);
        article.setContent(sanitize(article.getContent()));
        // Article không có @PreUpdate nên tự cập nhật mốc thời gian
        article.setUpdatedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    // Tít và chuyên mục bắt buộc ngay cả khi chỉ lưu nháp
    private static void validateDraft(ArticleDraftRequestDTO request) {
        if (request.getTitle() == null || request.getTitle().isBlank())
            throw new ApplicationException(ErrorCode.ARTICLE_TITLE_REQUIRED);
        if (request.getCategory() == null || request.getCategory().isBlank())
            throw new ApplicationException(ErrorCode.ARTICLE_CATEGORY_REQUIRED);
    }

    // Chuẩn hóa dữ liệu form trước khi mapper chép vào entity:
    // bỏ khoảng trắng hai đầu (chuỗi rỗng -> null), cắt theo độ dài cột DB, nội dung null -> ""
    private static void normalizeDraft(ArticleDraftRequestDTO request) {
        request.setTitle(truncate(trimToNull(request.getTitle()), 255));
        request.setSubtitle(truncate(trimToNull(request.getSubtitle()), 255));
        request.setSapo(trimToNull(request.getSapo()));
        request.setCategory(truncate(trimToNull(request.getCategory()), 100));
        request.setContent(request.getContent() == null ? "" : request.getContent());
    }

    // Kiểm tra quyền sửa bài: đúng Lead của bài, còn assignment LEAD hiệu lực trên đề tài,
    // và bài đang ở DRAFT / REVISION_REQUIRED. Sai điều kiện nào thì báo lỗi tương ứng.
    private Article editableArticle(Integer articleId, String email) {
        Account me = currentAccount(email);
        Article article = articles.findById(articleId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_NOT_FOUND));
        if (!article.getLead().getId().equals(me.getId()))
            throw new ApplicationException(ErrorCode.ARTICLE_FORBIDDEN);
        if (leadAssignment(article.getTopic().getId(), me.getId()).isEmpty())
            throw new ApplicationException(ErrorCode.ARTICLE_ASSIGNMENT_NOT_LEAD);
        if (!EDITABLE.contains(article.getStatus()))
            throw new ApplicationException(ErrorCode.ARTICLE_NOT_EDITABLE);
        return article;
    }

    // Kiểm tra quyền tạo bài: chỉ viết bài mới từ nhiệm vụ giao cho chính mình, vai trò viết chính, còn
    // hiệu lực, trên đề tài đã duyệt
    private Assignment writableAssignment(Integer assignmentId, Account me) {
        if (assignmentId == null)
            throw new ApplicationException(ErrorCode.ARTICLE_ASSIGNMENT_REQUIRED);
        return assignments.findWithDetailsById(assignmentId)
                .filter(a -> a.getAccount().getId().equals(me.getId()))
                .filter(a -> a.getAssignmentRole() == AssignmentRole.LEAD && a.getStatus() == AssignmentStatus.ASSIGNED)
                .filter(a -> a.getTopic().getStatus() == TopicStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_ASSIGNMENT_NOT_LEAD));
    }

    // Assignment LEAD còn hiệu lực (chưa REMOVED) của một người trên đề tài; rỗng nếu họ không phải Lead
    private Optional<Assignment> leadAssignment(Integer topicId, Integer accountId) {
        return assignments.findFirstByTopic_IdAndAccount_IdAndAssignmentRoleAndStatusNot(
                topicId, accountId, AssignmentRole.LEAD, AssignmentStatus.REMOVED);
    }

    // Tên Trưởng phòng đang phụ trách đề tài (người sẽ duyệt bài); null nếu chưa có
    private String reviewerName(Integer topicId) {
        return assignments
                .findFirstByTopic_IdAndAssignmentRoleAndStatusNot(topicId, AssignmentRole.DEPARTMENT_HEAD,
                        AssignmentStatus.REMOVED)
                .map(a -> a.getAccount().getFullName()).orElse(null);
    }

    // Tài khoản đang đăng nhập (tìm theo email); không thấy thì báo lỗi 403
    private Account currentAccount(String email) {
        if (email == null)
            throw new ApplicationException(ErrorCode.ARTICLE_FORBIDDEN);
        return accounts.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ApplicationException(ErrorCode.ARTICLE_FORBIDDEN));
    }

    // Lọc HTML nội dung bài: chỉ giữ thẻ / thuộc tính trong CONTENT_SAFELIST (chặn script, on*, javascript:)
    private static String sanitize(String html) {
        if (html == null || html.isBlank())
            return "";
        return Jsoup.clean(html, "http://localhost/", CONTENT_SAFELIST, OUTPUT);
    }

    // ===================== Tính dữ liệu hiển thị =====================

    // Tính nhãn vị trí và nhãn cấp duyệt của một góp ý rồi đưa cho mapper đóng gói.
    // replies: các trả lời của góp ý này (đã đóng gói)
    private ArticleEditorDTO.VersionComment toVersionComment(Comment comment, List<ArticleEditorDTO.VersionComment> replies) {
        return articleMapper.toVersionComment(comment, anchorLabel(comment), stageLabel(comment.getReview().getReviewStage()),
                replies);
    }

    // Nhãn hiển thị của phiên bản: dùng version_label ("1.0", "1.1"...), thiếu thì dùng số version
    private static String versionLabel(ArticleVersion version) {
        return version.getVersionLabel() != null ? version.getVersionLabel() : String.valueOf(version.getVersionNumber());
    }

    // Tư liệu là ảnh có đường dẫn (dùng làm ảnh đại diện / chèn vào bài được)
    private static boolean isImageSource(ArticleEditorDTO.Source source) {
        return source.type() == SourceType.IMAGE && source.fileUrl() != null && !source.fileUrl().isBlank();
    }

    // Chữ hiển thị của trạng thái bài
    private static String statusLabel(ArticleStatus status) {
        return switch (status) {
            case DRAFT -> "Bản thảo";
            case DEPARTMENT_REVIEW -> "Chờ trưởng ban duyệt";
            case REVISION_REQUIRED -> "Cần chỉnh sửa";
            case DEPARTMENT_APPROVED -> "Trưởng ban đã duyệt";
            case APPROVED -> "Đã duyệt xuất bản";
            case SCHEDULED -> "Đã lên lịch đăng";
            case PUBLISHED -> "Đã xuất bản";
        };
    }

    // Nhóm màu của trạng thái bài, giống bảng nhiệm vụ: doing (đang ở tay phóng viên) / review (đang duyệt) / done
    private static String statusGroup(ArticleStatus status) {
        return switch (status) {
            case DRAFT, REVISION_REQUIRED -> "doing";
            case DEPARTMENT_REVIEW, DEPARTMENT_APPROVED -> "review";
            case APPROVED, SCHEDULED, PUBLISHED -> "done";
        };
    }

    // Chữ hiển thị của mức ưu tiên đề tài (trang soạn bài); không có mức ưu tiên thì chuỗi rỗng
    private static String priorityLabel(Priority priority) {
        if (priority == null) return "";
        return switch (priority) {
            case LOW -> "Thấp";
            case NORMAL -> "Bình thường";
            case HIGH -> "Cao";
            case URGENT -> "Khẩn";
        };
    }

    // Chữ hiển thị của cấp duyệt
    private static String stageLabel(ReviewStage stage) {
        return stage == ReviewStage.DEPARTMENT ? "Trưởng ban" : "Thư ký tòa soạn";
    }

    // Chữ hiển thị của quyết định duyệt (null = lượt duyệt đang mở, chưa quyết định)
    private static String decisionLabel(ArticleReviewDecision decision) {
        if (decision == null) return "Đang duyệt";
        return decision == ArticleReviewDecision.APPROVED ? "Đã duyệt" : "Yêu cầu chỉnh sửa";
    }

    // Chữ mô tả vị trí góp ý: "Tiêu đề", "Sa-pô", "Đoạn 3"...; không gắn vị trí thì "Góp ý chung"
    private static String anchorLabel(Comment comment) {
        AnchorField field = comment.getAnchorField();
        if (field == null)
            return "Góp ý chung";
        return switch (field) {
            case TITLE -> "Tiêu đề";
            case SUBTITLE -> "Tiêu đề phụ";
            case SAPO -> "Sa-pô";
            case CONTENT -> comment.getAnchorParagraph() == null ? "Nội dung" : "Đoạn " + comment.getAnchorParagraph();
        };
    }

    // ===================== Hàm phụ: chuỗi =====================

    // Tên phòng ban của tài khoản (null nếu chưa gán phòng)
    private static String departmentName(Account account) {
        return account.getDepartment() == null ? null : account.getDepartment().getDepartmentName();
    }

    // Bỏ khoảng trắng hai đầu; chuỗi rỗng thì trả null
    private static String trimToNull(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Cắt chuỗi còn tối đa max ký tự (độ dài cột DB)
    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    // Chữ viết tắt cho avatar: "Nguyễn Thu Hà" -> "TH" (chữ cái đầu của hai từ cuối)
    private static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] words = fullName.trim().split("\\s+");
        return Arrays.stream(words, Math.max(0, words.length - 2), words.length)
                .map(w -> w.substring(0, 1))
                .collect(Collectors.joining())
                .toUpperCase(Locale.ROOT);
    }
}
