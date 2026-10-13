package com.group6.electronic_drafting_system.mapper.Article;

import com.group6.electronic_drafting_system.dto.Article.ArticleDraftRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleEditorDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleListItemDTO;
import com.group6.electronic_drafting_system.dto.Article.AssignedTopicDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.ArticleSource;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ArticleMapper {

    public ArticleListItemDTO toListItemDTO(Article article) {
        return new ArticleListItemDTO(article.getId(), article.getTitle(), article.getTopic().getTitle(),
                article.getCategory(), article.getStatus(), article.getUpdatedAt());
    }

    public AssignedTopicDTO toAssignedTopicDTO(Assignment assignment) {
        Topic topic = assignment.getTopic();
        return new AssignedTopicDTO(topic.getId(), topic.getTitle(), topic.getCategory(), assignment.getDeadline());
    }

    public ArticleEditorDTO.Version toVersionItem(ArticleVersion version) {
        return new ArticleEditorDTO.Version(version.getId(), version.getVersionNumber(), version.getVersionLabel(), version.getTitle(),
                version.getChangeSummary(), version.getCreator().getFullName(), version.getCreatedAt());
    }

    public ArticleEditorDTO.Source toSourceItem(ArticleSource articleSource) {
        return new ArticleEditorDTO.Source(articleSource.getId(), articleSource.getSource().getTitle(), articleSource.getSource().getSourceType(),
                articleSource.getSource().getFileUrl(), articleSource.getCaption(), articleSource.getNote(), Boolean.TRUE.equals(articleSource.getFeatured()));
    }

    public ArticleEditorDTO.Review toReviewItem(ArticleReview review) {
        return new ArticleEditorDTO.Review(review.getReviewer().getFullName(), review.getDecision(), review.getFeedback(), review.getReviewedAt());
    }

    public ArticleEditorDTO toEditorDTO(Article article, boolean editable, OffsetDateTime deadline, String reviewerName,
                                        ArticleEditorDTO.Review latestReview, List<ArticleEditorDTO.Version> versions,
                                        List<ArticleEditorDTO.Source> sources) {
        Topic topic = article.getTopic();
        Account lead = article.getLead();
        return new ArticleEditorDTO(article.getId(), article.getTitle(), article.getSubtitle(), article.getSapo(), article.getContent(),
                article.getCategory(), article.getStatus(), editable, article.getCreatedAt(), article.getUpdatedAt(), article.getSubmittedAt(),
                topic.getId(), topic.getTitle(), topic.getCategory(), topic.getPriority(), topic.getExpectedDate(),
                lead.getFullName(), departmentName(lead), reviewerName, deadline, latestReview, versions, sources);
    }

    // Trang "Viết bài mới": chưa có bản ghi article, lấy dữ liệu từ form (khi nhập lỗi) và đề tài đang chọn
    public ArticleEditorDTO toNewEditorDTO(ArticleDraftRequestDTO form, Topic topic, Account lead, OffsetDateTime deadline, String reviewerName) {
        String title = form.getTitle() != null ? form.getTitle() : topic != null ? topic.getTitle() : "";
        String category = form.getCategory() != null ? form.getCategory() : topic != null ? topic.getCategory() : "";
        return new ArticleEditorDTO(null, title, form.getSubtitle(), form.getSapo(), form.getContent() == null ? "" : form.getContent(),
                category, ArticleStatus.DRAFT, true, null, null, null,
                topic == null ? null : topic.getId(), topic == null ? null : topic.getTitle(), topic == null ? null : topic.getCategory(),
                topic == null ? null : topic.getPriority(), topic == null ? null : topic.getExpectedDate(),
                lead.getFullName(), departmentName(lead), reviewerName, deadline, null, List.of(), List.of());
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
