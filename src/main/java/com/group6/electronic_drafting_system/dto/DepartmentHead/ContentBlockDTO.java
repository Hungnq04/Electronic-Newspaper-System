package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.Builder;
import lombok.Getter;

// Một khối trong thân bài, giữ đúng thứ tự phóng viên đã sắp xếp: đoạn chữ, ảnh, video hoặc bảng
//
// TẠO Ở ĐÂU: ReviewWorkspaceMapper.blocks(content) (mapper/DepartmentHead/ReviewWorkspaceMapper.java dòng 119)
//            cắt nội dung HTML của version thành từng khối.
// NẰM TRONG: VersionViewDTO.blocks
// HTML ĐỌC:  workspacereview.html, khối id="article-body":
//            th:each="block : ${workspace.current.blocks}" th:switch="${block.kind.name()}" -> th:case 'TEXT' / 'IMAGE' / 'VIDEO' / 'TABLE'
@Getter
@Builder
public class ContentBlockDTO {
    // 4 loại khối. enum = danh sách giá trị cố định.
    public enum Kind { TEXT, IMAGE, VIDEO, TABLE }

    private Kind kind;
    // TEXT: thẻ gốc (p, h2, h3, h4, blockquote, pre, li) để chọn kiểu chữ khi hiển thị
    // HTML: th:classappend="|ws-${block.tag}|" -> class ws-p, ws-h2... (CSS chọn cỡ chữ theo class)
    private String tag;
    // TEXT: đoạn thứ mấy, đếm từ 1 = comment.anchor_paragraph. Ảnh / video / bảng không được đánh số
    // HTML: data-paragraph="${block.paragraph}" -> JS đọc khi bôi đen để biết góp ý thuộc đoạn nào
    private Integer paragraph;
    // TEXT: HTML đã lọc (chỉ giữ in đậm, nghiêng, link). TABLE: HTML bảng đã lọc
    // HTML: th:utext="${block.html}" (utext = in nguyên HTML, không đổi < > thành chữ; an toàn vì đã lọc ở mapper)
    private String html;
    // TEXT: chữ thuần của đoạn, dùng để kiểm tra vị trí góp ý và so sánh phiên bản
    private String text;
    // IMAGE / VIDEO: link Cloudinary (chỉ nhận https) và chú thích
    private String url;       // HTML: <img th:src="${block.url}"> hoặc <video th:src="${block.url}">
    private String caption;   // HTML: <figcaption th:text="${block.caption}">
}
