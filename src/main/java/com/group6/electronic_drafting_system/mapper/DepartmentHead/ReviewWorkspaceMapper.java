package com.group6.electronic_drafting_system.mapper.DepartmentHead;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ReviewCommentDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.SourceItemDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.VersionViewDTO;
import com.group6.electronic_drafting_system.entity.ArticleSource;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import com.group6.electronic_drafting_system.entity.Comment;
import com.group6.electronic_drafting_system.entity.Source;
import com.group6.electronic_drafting_system.entity.enums.AnchorField;
import com.group6.electronic_drafting_system.entity.enums.CommentStatus;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReviewWorkspaceMapper {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    public VersionViewDTO toVersionView(ArticleVersion version) {
        return VersionViewDTO.builder()
                .versionId(version.getId())
                .label(version.getVersionLabel())
                .createdAtText(version.getCreatedAt() != null ? version.getCreatedAt().format(DATE_TIME) : "")
                .changeSummary(version.getChangeSummary())
                .title(normalize(version.getTitle()))
                .subtitle(normalize(version.getSubtitle()))
                .sapo(normalize(version.getSapo()))
                .paragraphs(paragraphs(version.getContent()))
                .build();
    }

    // replies: các câu trả lời (comment có parent) thuộc comment này
    public ReviewCommentDTO toComment(Comment comment, List<Comment> replies) {
        return ReviewCommentDTO.builder()
                .id(comment.getId())
                .anchorLabel(anchorLabel(comment.getAnchorField(), comment.getAnchorParagraph()))
                .anchorField(comment.getAnchorField() != null ? comment.getAnchorField().name() : null)
                .anchorParagraph(comment.getAnchorParagraph())
                .anchorStart(comment.getAnchorStart())
                .anchorEnd(comment.getAnchorEnd())
                .quotedText(comment.getQuotedText())
                .content(comment.getContent())
                .authorName(comment.getCreator().getFullName())
                .timeText(comment.getCreatedAt() != null ? comment.getCreatedAt().format(TIME) : "")
                .resolved(comment.getStatus() == CommentStatus.RESOLVED)
                .replies(replies.stream().map(reply -> toComment(reply, List.of())).toList())
                .build();
    }

    public SourceItemDTO toSource(ArticleSource articleSource) {
        Source source = articleSource.getSource();
        String typeLabel = switch (source.getSourceType()) {
            case IMAGE -> "Ảnh";
            case DOCUMENT -> "Tài liệu";
            case WEBSITE -> "Website";
            case INTERVIEW -> "Phỏng vấn";
            case INTERNAL_DOC -> "Tài liệu nội bộ";
        };
        return new SourceItemDTO(source.getTitle(), typeLabel, source.getFileUrl(),
                source.getSourceType() == SourceType.IMAGE,
                articleSource.getCaption(), Boolean.TRUE.equals(articleSource.getFeatured()));
    }

    // Trong DB các đoạn nội dung cách nhau bằng một dòng trống
    public List<String> paragraphs(String content) {
        if (content == null || content.isBlank()) return List.of();
        return Arrays.stream(normalize(content).split("\\n\\s*\\n"))
                .map(String::trim)
                .filter(paragraph -> !paragraph.isEmpty())
                .toList();
    }

    // Đoạn văn mà comment trỏ tới (dùng để kiểm tra vị trí và lấy quoted_text); null nếu không tồn tại
    public String anchorText(ArticleVersion version, AnchorField field, int paragraph) {
        if (field == AnchorField.CONTENT) {
            List<String> list = paragraphs(version.getContent());
            return paragraph >= 1 && paragraph <= list.size() ? list.get(paragraph - 1) : null;
        }
        if (paragraph != 1) return null; // tít / tít phụ / sapo chỉ có một đoạn
        return switch (field) {
            case TITLE -> normalize(version.getTitle());
            case SUBTITLE -> normalize(version.getSubtitle());
            case SAPO -> normalize(version.getSapo());
            default -> null;
        };
    }

    public static String anchorLabel(AnchorField field, Integer paragraph) {
        if (field == null) return "Góp ý chung";
        return switch (field) {
            case TITLE -> "Tít";
            case SUBTITLE -> "Tít phụ";
            case SAPO -> "Sapo";
            case CONTENT -> "Đoạn " + paragraph;
        };
    }

    // Trình duyệt đổi \r\n thành \n khi hiển thị, nên phía server cũng đổi để vị trí ký tự khớp với JS
    private static String normalize(String text) {
        return text == null ? null : text.replace("\r\n", "\n").trim();
    }
}
