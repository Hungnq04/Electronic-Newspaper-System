package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.Builder;
import lombok.Getter;

// Một khối trong thân bài, giữ đúng thứ tự phóng viên đã sắp xếp: đoạn chữ, ảnh, video hoặc bảng
@Getter
@Builder
public class ContentBlockDTO {
    public enum Kind { TEXT, IMAGE, VIDEO, TABLE }

    private Kind kind;
    // TEXT: thẻ gốc (p, h2, h3, h4, blockquote, pre, li) để chọn kiểu chữ khi hiển thị
    private String tag;
    // TEXT: đoạn thứ mấy, đếm từ 1 = comment.anchor_paragraph. Ảnh / video / bảng không được đánh số
    private Integer paragraph;
    // TEXT: HTML đã lọc (chỉ giữ in đậm, nghiêng, link). TABLE: HTML bảng đã lọc
    private String html;
    // TEXT: chữ thuần của đoạn, dùng để kiểm tra vị trí góp ý và so sánh phiên bản
    private String text;
    // IMAGE / VIDEO: link Cloudinary (chỉ nhận https) và chú thích
    private String url;
    private String caption;
}
