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
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import java.util.List;
import org.springframework.stereotype.Component;

// Chỉ chép dữ liệu giữa entity và DTO. Các giá trị cần tính (nhãn, chuẩn hóa chuỗi, lọc ảnh / video...)
// do ArticleServiceImpl tính rồi truyền vào.
@Component
public class ArticleMapper {

    public ArticleListItemDTO toListItemDTO(Article article, String statusLabel, String statusGroup) {
        return new ArticleListItemDTO(article.getId(), article.getTitle(), article.getTopic().getTitle(),
                article.getCategory(), article.getStatus(), statusLabel, statusGroup, article.getUpdatedAt());
    }

    public AssignedTopicDTO toAssignedTopicDTO(Assignment assignment, long articleCount) {
        Topic topic = assignment.getTopic();
        return new AssignedTopicDTO(assignment.getId(), topic.getId(), topic.getTitle(), topic.getCategory(),
                assignment.getDeadline(), articleCount);
    }

    // reviews / comments: lượt duyệt và comment gốc (kèm trả lời) thuộc riêng phiên bản này
    // view: nội dung phiên bản đã chia đoạn (ReviewWorkspaceMapper.toVersionView) để tô vàng góp ý
    public ArticleEditorDTO.Version toVersionItem(ArticleVersion version, String label,
            List<ArticleEditorDTO.VersionReview> reviews,
            List<ArticleEditorDTO.VersionComment> comments, VersionViewDTO view) {
        return new ArticleEditorDTO.Version(version.getId(), version.getVersionNumber(), version.getVersionLabel(), label,
                version.getTitle(),
                version.getSubtitle(), version.getSapo(), version.getContent(),
                version.getChangeSummary(), version.getCreator().getFullName(), version.getCreatedAt(), reviews,
                comments, view);
    }

    public ArticleEditorDTO.VersionReview toVersionReview(ArticleReview review, String stageLabel, String decisionLabel) {
        return new ArticleEditorDTO.VersionReview(review.getReviewer().getFullName(), stageLabel,
                review.getReviewStage(), review.getDecision(), decisionLabel, review.getFeedback(), review.getReviewedAt());
    }

    // Kèm vị trí góp ý (field, đoạn, ký tự bắt đầu / kết thúc) và cấp của người góp ý (Trưởng ban / Thư ký)
    public ArticleEditorDTO.VersionComment toVersionComment(Comment comment, String anchorLabel, String stageLabel,
            List<ArticleEditorDTO.VersionComment> replies) {
        return new ArticleEditorDTO.VersionComment(comment.getId(), anchorLabel, comment.getAnchorField(),
                comment.getAnchorParagraph(), comment.getAnchorStart(), comment.getAnchorEnd(),
                comment.getQuotedText(), comment.getContent(),
                comment.getCreator().getFullName(), stageLabel, comment.getReview().getReviewStage(), comment.getStatus(),
                comment.getCreatedAt(), replies);
    }

    public ArticleEditorDTO.TopicMedia toTopicMedia(Source source, boolean video, String note, String addedByName, String caption) {
        return new ArticleEditorDTO.TopicMedia(source.getId(), source.getTitle(), source.getFileUrl(), video,
                note, addedByName, caption);
    }

    public ArticleEditorDTO.Source toSourceItem(ArticleSource articleSource) {
        return new ArticleEditorDTO.Source(articleSource.getId(), articleSource.getSource().getTitle(),
                articleSource.getSource().getSourceType(),
                articleSource.getSource().getFileUrl(), articleSource.getCaption(), articleSource.getNote(),
                Boolean.TRUE.equals(articleSource.getFeatured()));
    }

    public ArticleEditorDTO.Review toReviewItem(ArticleReview review, String stageLabel) {
        return new ArticleEditorDTO.Review(review.getReviewer().getFullName(), stageLabel,
                review.getDecision(), review.getFeedback(), review.getReviewedAt());
    }

    // assignment: nhiệm vụ viết chính hiện hành của người dùng trên đề tài (null nếu đã bị gỡ)
    public ArticleEditorDTO toEditorDTO(Article article, boolean editable, Assignment assignment,
            String statusLabel, String priorityLabel, String leadInitials, String departmentName, String reviewerName,
            ArticleEditorDTO.Review latestReview, List<ArticleEditorDTO.Version> versions,
            ArticleEditorDTO.Version returnedVersion, String restoredFromLabel,
            List<ArticleEditorDTO.Source> sources, ArticleEditorDTO.Source featuredImage,
            List<ArticleEditorDTO.Source> imageSources, List<ArticleEditorDTO.TopicMedia> topicMedia) {
        Topic topic = article.getTopic();
        return new ArticleEditorDTO(article.getId(), article.getTitle(), article.getSubtitle(), article.getSapo(),
                article.getContent(), article.getCategory(),
                article.getStatus(), statusLabel, editable, false,
                article.getCreatedAt(), article.getUpdatedAt(), article.getSubmittedAt(),
                assignment == null ? null : assignment.getId(), topic.getId(), topic.getTitle(), topic.getCategory(),
                topic.getPriority(), priorityLabel, topic.getExpectedDate(),
                article.getLead().getFullName(), leadInitials, departmentName, reviewerName,
                assignment == null ? null : assignment.getDeadline(),
                latestReview, versions, returnedVersion, restoredFromLabel,
                sources, featuredImage, imageSources, topicMedia);
    }

    // Trang "Viết bài mới": chưa có bản ghi article. title / category / content đã được service chọn sẵn
    // (lấy từ form khi nhập lỗi, không có thì lấy theo đề tài của nhiệm vụ)
    public ArticleEditorDTO toNewEditorDTO(ArticleDraftRequestDTO form, Assignment assignment, Account lead,
            String title, String category, String content,
            String statusLabel, String priorityLabel, String leadInitials, String departmentName, String reviewerName,
            List<ArticleEditorDTO.TopicMedia> topicMedia) {
        Topic topic = assignment.getTopic();
        return new ArticleEditorDTO(null, title, form.getSubtitle(), form.getSapo(), content, category,
                ArticleStatus.DRAFT, statusLabel, true, true,
                null, null, null,
                assignment.getId(), topic.getId(), topic.getTitle(), topic.getCategory(),
                topic.getPriority(), priorityLabel, topic.getExpectedDate(),
                lead.getFullName(), leadInitials, departmentName, reviewerName, assignment.getDeadline(),
                null, List.of(), null, null,
                List.of(), null, List.of(), topicMedia);
    }

    // Tạo entity bài mới; request đã được service chuẩn hóa (normalizeDraft) trước khi gọi
    public Article toEntity(ArticleDraftRequestDTO request, Topic topic, Account lead) {
        Article article = new Article();
        article.setTopic(topic);
        article.setLead(lead);
        updateEntity(request, article);
        return article;
    }

    // Chép dữ liệu soạn thảo vào entity; request đã được service chuẩn hóa (bỏ khoảng trắng, cắt độ dài cột)
    // và content được service lọc HTML sau đó
    public void updateEntity(ArticleDraftRequestDTO request, Article article) {
        article.setTitle(request.getTitle());
        article.setSubtitle(request.getSubtitle());
        article.setSapo(request.getSapo());
        article.setCategory(request.getCategory());
        article.setContent(request.getContent());
    }

    // Bản chụp cố định của bài khi nộp; changeSummary đã được service bỏ khoảng trắng
    public ArticleVersion toVersion(Article article, int versionNumber, String changeSummary) {
        ArticleVersion version = new ArticleVersion();
        version.setArticle(article);
        version.setCreator(article.getLead());
        version.setVersionNumber(versionNumber);
        version.setTitle(article.getTitle());
        version.setSubtitle(article.getSubtitle());
        version.setSapo(article.getSapo());
        version.setContent(article.getContent());
        version.setChangeSummary(changeSummary);
        return version;
    }
}
