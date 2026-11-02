package com.group6.electronic_drafting_system.mapper.Article;

import com.group6.electronic_drafting_system.dto.Article.ArticleDraftRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleEditorDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleListItemDTO;
import com.group6.electronic_drafting_system.dto.Article.AssignedTopicDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;
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
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import com.group6.electronic_drafting_system.service.MediaStorageService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ArticleMapper {

    public ArticleListItemDTO toListItemDTO(Article article) {
        return new ArticleListItemDTO(article.getId(), article.getTitle(), article.getTopic().getTitle(),
                article.getCategory(), article.getStatus(), article.getUpdatedAt());
    }

    public AssignedTopicDTO toAssignedTopicDTO(Assignment assignment, long articleCount) {
        Topic topic = assignment.getTopic();
        return new AssignedTopicDTO(assignment.getId(), topic.getId(), topic.getTitle(), topic.getCategory(), assignment.getDeadline(), articleCount);
    }

    // reviews / comments: lượt duyệt và comment gốc (kèm trả lời) thuộc riêng phiên bản này
    // view: nội dung phiên bản đã chia đoạn (ReviewWorkspaceMapper.toVersionView) để tô vàng góp ý
    public ArticleEditorDTO.Version toVersionItem(ArticleVersion version, List<ArticleEditorDTO.VersionReview> reviews,
                                                  List<ArticleEditorDTO.VersionComment> comments, VersionViewDTO view) {
        return new ArticleEditorDTO.Version(version.getId(), version.getVersionNumber(), version.getVersionLabel(), version.getTitle(),
                version.getSubtitle(), version.getSapo(), version.getContent(),
                version.getChangeSummary(), version.getCreator().getFullName(), version.getCreatedAt(), reviews, comments, view);
    }

    public ArticleEditorDTO.VersionReview toVersionReview(ArticleReview review) {
        return new ArticleEditorDTO.VersionReview(review.getReviewer().getFullName(), stageLabel(review.getReviewStage()),
                review.getReviewStage(), review.getDecision(), review.getFeedback(), review.getReviewedAt());
    }

    // Kèm vị trí góp ý (field, đoạn, ký tự bắt đầu / kết thúc) và cấp của người góp ý (Trưởng ban / Thư ký)
    public ArticleEditorDTO.VersionComment toVersionComment(Comment comment, List<ArticleEditorDTO.VersionComment> replies) {
        ReviewStage stage = comment.getReview().getReviewStage();
        return new ArticleEditorDTO.VersionComment(comment.getId(), anchorLabel(comment), comment.getAnchorField(),
                comment.getAnchorParagraph(), comment.getAnchorStart(), comment.getAnchorEnd(),
                comment.getQuotedText(), comment.getContent(),
                comment.getCreator().getFullName(), stageLabel(stage), stage, comment.getStatus(),
                comment.getCreatedAt(), replies);
    }

    // null nếu tư liệu không phải ảnh / video
    public ArticleEditorDTO.TopicMedia toTopicMedia(TopicSource topicSource) {
        Source source = topicSource.getSource();
        boolean video = source.getSourceType() != SourceType.IMAGE && MediaStorageService.isVideoUrl(source.getFileUrl());
        boolean image = source.getSourceType() == SourceType.IMAGE && source.getFileUrl() != null && !source.getFileUrl().isBlank();
        if (!image && !video) return null;
        return new ArticleEditorDTO.TopicMedia(source.getId(), source.getTitle(), source.getFileUrl(), video, trimToNull(topicSource.getNote()),
                source.getAddedBy() == null ? null : source.getAddedBy().getFullName());
    }

    public ArticleEditorDTO.Source toSourceItem(ArticleSource articleSource) {
        return new ArticleEditorDTO.Source(articleSource.getId(), articleSource.getSource().getTitle(), articleSource.getSource().getSourceType(),
                articleSource.getSource().getFileUrl(), articleSource.getCaption(), articleSource.getNote(), Boolean.TRUE.equals(articleSource.getFeatured()));
    }

    public ArticleEditorDTO.Review toReviewItem(ArticleReview review) {
        return new ArticleEditorDTO.Review(review.getReviewer().getFullName(), stageLabel(review.getReviewStage()),
                review.getDecision(), review.getFeedback(), review.getReviewedAt());
    }

    // assignment: nhiệm vụ viết chính hiện hành của người dùng trên đề tài (null nếu đã bị gỡ)
    public ArticleEditorDTO toEditorDTO(Article article, boolean editable, Assignment assignment, String reviewerName,
                                        ArticleEditorDTO.Review latestReview, List<ArticleEditorDTO.Version> versions,
                                        ArticleEditorDTO.Version returnedVersion,
                                        List<ArticleEditorDTO.Source> sources, List<ArticleEditorDTO.TopicMedia> topicMedia) {
        Topic topic = article.getTopic();
        Account lead = article.getLead();
        return new ArticleEditorDTO(article.getId(), article.getTitle(), article.getSubtitle(), article.getSapo(), article.getContent(),
                article.getCategory(), article.getStatus(), editable, article.getCreatedAt(), article.getUpdatedAt(), article.getSubmittedAt(),
                assignment == null ? null : assignment.getId(), topic.getId(), topic.getTitle(), topic.getCategory(), topic.getPriority(), topic.getExpectedDate(),
                lead.getFullName(), departmentName(lead), reviewerName, assignment == null ? null : assignment.getDeadline(),
                latestReview, versions, returnedVersion, sources, topicMedia);
    }

    // Trang "Viết bài mới": chưa có bản ghi article, lấy dữ liệu từ form (khi nhập lỗi) và đề tài của nhiệm vụ
    public ArticleEditorDTO toNewEditorDTO(ArticleDraftRequestDTO form, Assignment assignment, Account lead, String reviewerName,
                                           List<ArticleEditorDTO.TopicMedia> topicMedia) {
        Topic topic = assignment.getTopic();
        String title = form.getTitle() != null ? form.getTitle() : topic.getTitle();
        String category = form.getCategory() != null ? form.getCategory() : topic.getCategory();
        return new ArticleEditorDTO(null, title, form.getSubtitle(), form.getSapo(), form.getContent() == null ? "" : form.getContent(),
                category, ArticleStatus.DRAFT, true, null, null, null,
                assignment.getId(), topic.getId(), topic.getTitle(), topic.getCategory(), topic.getPriority(), topic.getExpectedDate(),
                lead.getFullName(), departmentName(lead), reviewerName, assignment.getDeadline(), null, List.of(), null, List.of(), topicMedia);
    }

    public Article toEntity(ArticleDraftRequestDTO request, Topic topic, Account lead) {
        Article article = new Article();
        article.setTopic(topic);
        article.setLead(lead);
        updateEntity(request, article);
        return article;
    }

    // Chép dữ liệu soạn thảo vào entity (đã chuẩn hóa khoảng trắng / độ dài cột); content được service lọc HTML sau đó
    public void updateEntity(ArticleDraftRequestDTO request, Article article) {
        article.setTitle(truncate(trimToNull(request.getTitle()), 255));
        article.setSubtitle(truncate(trimToNull(request.getSubtitle()), 255));
        article.setSapo(trimToNull(request.getSapo()));
        article.setCategory(truncate(trimToNull(request.getCategory()), 100));
        article.setContent(request.getContent() == null ? "" : request.getContent());
    }

    // Bản chụp cố định của bài khi nộp
    public ArticleVersion toVersion(Article article, int versionNumber, String changeSummary) {
        ArticleVersion version = new ArticleVersion();
        version.setArticle(article);
        version.setCreator(article.getLead());
        version.setVersionNumber(versionNumber);
        version.setTitle(article.getTitle());
        version.setSubtitle(article.getSubtitle());
        version.setSapo(article.getSapo());
        version.setContent(article.getContent());
        version.setChangeSummary(trimToNull(changeSummary));
        return version;
    }

    private static String stageLabel(ReviewStage stage) {
        return stage == ReviewStage.DEPARTMENT ? "Trưởng ban" : "Thư ký tòa soạn";
    }

    private static String anchorLabel(Comment comment) {
        AnchorField field = comment.getAnchorField();
        if (field == null) return "Góp ý chung";
        return switch (field) {
            case TITLE -> "Tiêu đề";
            case SUBTITLE -> "Tiêu đề phụ";
            case SAPO -> "Sa-pô";
            case CONTENT -> comment.getAnchorParagraph() == null ? "Nội dung" : "Đoạn " + comment.getAnchorParagraph();
        };
    }

    private static String departmentName(Account account) {
        return account.getDepartment() == null ? null : account.getDepartment().getDepartmentName();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
