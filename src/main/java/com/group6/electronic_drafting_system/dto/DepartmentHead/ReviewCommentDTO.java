package com.group6.electronic_drafting_system.dto.DepartmentHead;

import lombok.Builder;
import lombok.Getter;

// Một góp ý gốc của người duyệt (Trưởng ban hoặc Thư ký)
//
// TẠO Ở ĐÂU: ReviewWorkspaceMapper.toComment(comment) (mapper/DepartmentHead/ReviewWorkspaceMapper.java),
//            được DepartmentHeadReviewServiceImpl.rootComments() gọi cho từng góp ý gốc.
// NẰM TRONG: ReviewWorkspaceDTO.comments (góp ý trên bản đang duyệt)
// HTML ĐỌC:  th:each="comment : ${workspace.comments}"
//
// DTO dùng để HIỂN THỊ góp ý. Còn dữ liệu form GỬI LÊN khi thêm góp ý là CommentRequestDTO (file khác).
@Getter
@Builder
public class ReviewCommentDTO {
    private Integer id;            // mã comment; HTML ghép vào URL form Xóa / Đã sửa và vào data-cid
    private String anchorLabel;    // nhãn vị trí bằng chữ, ví dụ "Đoạn 2", "Sapo", "Góp ý chung" -> ${comment.anchorLabel}
    // TITLE / SUBTITLE / SAPO / CONTENT, null = góp ý chung (không gắn vị trí)
    // HTML: data-field="${comment.anchorField}"; null thì không có nút "Xem vị trí →"
    private String anchorField;
    // 4 field vị trí dưới đây được HTML in ra thành data-paragraph / data-start / data-end trên thẻ góp ý,
    // JS dùng để tô vàng đúng chữ được góp ý trong bài.
    private Integer anchorParagraph;   // đoạn thứ mấy (đếm từ 1)
    private Integer anchorStart;       // ký tự bắt đầu (đếm từ 0)
    private Integer anchorEnd;         // ký tự kết thúc (không tính ký tự này)
    private String quotedText;         // đoạn chữ được bôi đen -> hiện trong ngoặc kép
    private String content;            // nội dung góp ý -> ${comment.content}
    private String authorName;         // người viết
    private String timeText;           // thời gian, đã định dạng sẵn
    private String versionLabel;       // góp ý nằm trên phiên bản nào, ví dụ "1.0"
    // Không có trạng thái "đã sửa / chưa sửa" và không có câu trả lời: màn này chỉ hiện lại góp ý đã viết.
}
