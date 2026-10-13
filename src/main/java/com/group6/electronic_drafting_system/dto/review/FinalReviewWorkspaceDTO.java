package com.group6.electronic_drafting_system.dto.review;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

// Dữ liệu màn Final Article Review Workspace: nội dung version mới nhất + bối cảnh để Thư ký quyết định
@Value @Builder
public class FinalReviewWorkspaceDTO {
    Integer articleId;
    String status;
    String statusLabel;
    String versionLabel;
    String changeSummary;
    OffsetDateTime submittedAt;
    String title;
    String subtitle;
    String sapo;
    List<String> paragraphs;
    String category;
    String featuredImageUrl;
    String featuredImageCaption;

    String topicTitle;
    String leadName;
    List<TeamMember> team;
    List<SourceItem> sources;
    long revisionRounds;

    List<ReviewItem> reviewHistory;
    List<CommentItem> currentVersionComments;
    List<CommentItem> earlierOpenComments;

    // Chỉ true khi bài đang DEPARTMENT_APPROVED và lượt duyệt đang mở thuộc về người xem
    boolean canDecide;
    // Lý do không được quyết định (null nếu canDecide hoặc bài đã qua bước này)
    String blockedReason;

    @Value
    public static class TeamMember {
        String name;
        String roleLabel;
        String statusLabel;
        String requirements;
        OffsetDateTime deadline;
    }

    @Value
    public static class SourceItem {
        String title;
        String typeLabel;
        String url;
        String caption;
        String note;
        boolean featured;
        boolean internalOnly;
    }

    @Value
    public static class ReviewItem {
        String versionLabel;
        String stageLabel;
        String kindLabel;
        String decision;
        String decisionLabel;
        String reviewerName;
        String feedback;
        OffsetDateTime reviewedAt;
    }

    @Value
    public static class CommentItem {
        String versionLabel;
        String stageLabel;
        String anchorLabel;
        String quotedText;
        String content;
        String authorName;
        String status;
        OffsetDateTime createdAt;
        List<CommentItem> replies;
    }
}
