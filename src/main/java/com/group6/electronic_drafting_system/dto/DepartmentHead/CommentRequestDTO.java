package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.Getter;
import lombok.Setter;

// Dữ liệu form "Thêm góp ý". anchorField để trống = góp ý chung cho cả bài
@Getter
@Setter
public class CommentRequestDTO {
    private String anchorField;
    private Integer anchorParagraph;
    private Integer anchorStart;
    private Integer anchorEnd;
    private String content;
}
