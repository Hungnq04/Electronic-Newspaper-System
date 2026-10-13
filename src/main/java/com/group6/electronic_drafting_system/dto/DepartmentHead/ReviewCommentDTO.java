package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// Một góp ý gốc của người duyệt, kèm các câu trả lời của Lead (nếu có)
@Getter
@Builder
public class ReviewCommentDTO {
    private Integer id;
    private String anchorLabel;
    // TITLE / SUBTITLE / SAPO / CONTENT, null = góp ý chung (không gắn vị trí)
    private String anchorField;
    private Integer anchorParagraph;
    private Integer anchorStart;
    private Integer anchorEnd;
    private String quotedText;
    private String content;
    private String authorName;
    private String timeText;
    private boolean resolved;
    private List<ReviewCommentDTO> replies;
}
