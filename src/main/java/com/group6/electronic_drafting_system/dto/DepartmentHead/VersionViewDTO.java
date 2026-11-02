package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// Một phiên bản bài (article_version) đã tách nội dung thành từng đoạn để hiển thị và so sánh
//
// TẠO Ở ĐÂU: ReviewWorkspaceMapper.toVersionView(version) (mapper/DepartmentHead/ReviewWorkspaceMapper.java dòng 52),
//            được DepartmentHeadReviewServiceImpl.openWorkspace() gọi cho bản đang duyệt, bản liền trước và mọi version.
// NẰM TRONG: ReviewWorkspaceDTO.current / previous / versions
// HTML ĐỌC:  ${w.current.title}, ${w.previous.label}, th:each="v : ${w.versions}"...
@Getter
@Builder
public class VersionViewDTO {
    private Integer versionId;       // mã version trong DB; HTML so sánh để biết version nào đang duyệt / liền trước
    private String label;            // nhãn hiển thị "1.0", "1.1"... -> HTML ghép thành "v1.1"
    private String createdAtText;    // thời điểm nộp, đã định dạng sẵn -> "Gửi duyệt: ..."
    private String changeSummary;    // ghi chú "đã sửa gì" của Lead khi nộp lại; null ở lần nộp đầu
    private String title;            // tít bài -> <h1 data-field="TITLE">
    private String subtitle;         // tít phụ -> <p data-field="SUBTITLE"> (ẩn nếu rỗng)
    private String sapo;             // sapo -> <p data-field="SAPO">
    // Chữ của từng đoạn (dùng cho so sánh phiên bản)
    // HTML in vào khối ẩn #version-data (mỗi đoạn là một <span data-key="Đoạn N">), JS đọc ra để so sánh.
    private List<String> paragraphs;
    // Thân bài theo đúng bố cục phóng viên sắp xếp (đoạn, ảnh, video, bảng)
    // HTML lặp: th:each="b : ${w.current.blocks}" rồi th:switch theo b.kind để in đoạn chữ / ảnh / video / bảng.
    private List<ContentBlockDTO> blocks;
}
