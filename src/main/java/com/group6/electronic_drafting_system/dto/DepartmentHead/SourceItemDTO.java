package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Một tư liệu gắn vào bài (article_source + source)
//
// TẠO Ở ĐÂU: ReviewWorkspaceMapper.toSource(articleSource, bodyUrls) (mapper/DepartmentHead/ReviewWorkspaceMapper.java dòng 86),
//            được DepartmentHeadReviewServiceImpl.openWorkspace() (BƯỚC 4) gọi cho từng tư liệu.
// NẰM TRONG: ReviewWorkspaceDTO.sources (danh sách) và ReviewWorkspaceDTO.featuredImage (ảnh đại diện)
// HTML ĐỌC:  khung "Tư liệu đính kèm": th:each="s : ${w.sources}" -> ${s.title}, ${s.url}...
//
// @AllArgsConstructor: constructor nhận đủ 7 field THEO ĐÚNG THỨ TỰ khai báo bên dưới.
@Getter
@AllArgsConstructor
public class SourceItemDTO {
    private String title;       // tên tư liệu -> ${s.title}
    private String typeLabel;   // loại bằng chữ (Ảnh, Tài liệu, Website...) -> hiện khi không có caption
    private String url;         // link Cloudinary / website; bấm thẻ tư liệu sẽ mở link này ở tab mới
    private boolean image;      // true = tư liệu là ảnh -> hiện ảnh thu nhỏ; false -> hiện icon file. Service dùng để đếm imageCount
    private String caption;     // chú thích ảnh
    private boolean featured;   // true = ảnh đại diện của bài -> hiện nhãn "Ảnh đại diện"
    // true = link của tư liệu này xuất hiện trong thân bài (ảnh / video đã được chèn vào nội dung)
    // HTML: nhãn "Có trong bài" (true) hoặc "Chưa dùng trong bài" (false)
    private boolean inBody;
}
