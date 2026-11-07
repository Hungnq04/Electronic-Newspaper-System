package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// Một góp ý gốc của người duyệt, kèm các câu trả lời của Lead (nếu có)
//
// TẠO Ở ĐÂU: ReviewWorkspaceMapper.toComment(comment, replies) (mapper/DepartmentHead/ReviewWorkspaceMapper.java dòng 68),
//            được DepartmentHeadReviewServiceImpl.rootComments() gọi cho từng góp ý gốc.
// NẰM TRONG: ReviewWorkspaceDTO.comments (vòng này) và ReviewWorkspaceDTO.previousComments (vòng trước)
// HTML ĐỌC:  th:each="c : ${w.comments}" (tab "Vòng này") và th:each="c : ${w.previousComments}" (tab "Vòng trước")
//
// DTO dùng để HIỂN THỊ góp ý. Còn dữ liệu form GỬI LÊN khi thêm góp ý là CommentRequestDTO (file khác).
@Getter
@Builder
public class ReviewCommentDTO {
    private Integer id;            // mã comment; HTML ghép vào URL form Xóa / Đã sửa và vào data-cid
    private String anchorLabel;    // nhãn vị trí bằng chữ, ví dụ "Đoạn 2", "Sapo", "Góp ý chung" -> ${c.anchorLabel}
    // TITLE / SUBTITLE / SAPO / CONTENT, null = góp ý chung (không gắn vị trí)
    // HTML: data-field="${c.anchorField}"; null thì không có nút "Xem vị trí →"
    private String anchorField;
    // 4 field vị trí dưới đây được HTML in ra thành data-paragraph / data-start / data-end trên thẻ góp ý,
    // JS dùng để tô vàng đúng chữ được góp ý trong bài.
    private Integer anchorParagraph;   // đoạn thứ mấy (đếm từ 1)
    private Integer anchorStart;       // ký tự bắt đầu (đếm từ 0)
    private Integer anchorEnd;         // ký tự kết thúc (không tính ký tự này)
    private String quotedText;         // đoạn chữ được bôi đen -> hiện trong ngoặc kép
    private String content;            // nội dung góp ý -> ${c.content}
    private String authorName;         // người viết
    private String timeText;           // thời gian, đã định dạng sẵn
    private boolean resolved;          // true = Lead đã sửa (comment.status = RESOLVED) -> nhãn "Đã sửa" / "Chưa sửa" ở tab "Vòng trước"
    private String versionLabel;       // góp ý nằm trên phiên bản nào, ví dụ "1.0" -> tab "Vòng trước" ghi rõ "Từ v1.0"
    // Các câu trả lời của góp ý này (mỗi câu cũng là một ReviewCommentDTO). HTML: th:each="r : ${c.replies}"
    private List<ReviewCommentDTO> replies;
}
