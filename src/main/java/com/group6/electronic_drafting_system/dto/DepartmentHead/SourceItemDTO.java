package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Một tư liệu gắn vào bài (article_source + source)
@Getter
@AllArgsConstructor
public class SourceItemDTO {
    private String title;
    private String typeLabel;
    private String url;
    private boolean image;
    private String caption;
    private boolean featured;
    // true = link của tư liệu này xuất hiện trong thân bài (ảnh / video đã được chèn vào nội dung)
    private boolean inBody;
}
