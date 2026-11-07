package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// Một phiên bản bài (article_version) đã tách nội dung thành từng đoạn để hiển thị và so sánh
@Getter
@Builder
public class VersionViewDTO {
    private Integer versionId;
    private String label;
    private String createdAtText;
    private String changeSummary;
    private String title;
    private String subtitle;
    private String sapo;
    // Chữ của từng đoạn (dùng cho so sánh phiên bản)
    private List<String> paragraphs;
    // Thân bài theo đúng bố cục phóng viên sắp xếp (đoạn, ảnh, video, bảng)
    private List<ContentBlockDTO> blocks;
}
