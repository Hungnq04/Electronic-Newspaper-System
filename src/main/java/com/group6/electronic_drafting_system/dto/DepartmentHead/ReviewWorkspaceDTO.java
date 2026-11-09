package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

// Toàn bộ dữ liệu của màn "Thẩm định bản thảo" (department_head/workspacereview.html)
//
// DTO = "cái hộp" chỉ chứa dữ liệu, không làm gì. Hộp này đi qua 3 nơi:
//   1. TẠO + ĐIỀN: DepartmentHeadReviewServiceImpl.openWorkspace() (BƯỚC 5) dùng builder: ReviewWorkspaceDTO.builder()....build()
//   2. CHUYỂN:     DepartmentHeadController.reviewWorkspace(): model.addAttribute("workspace", hộp này)
//   3. ĐỌC:        workspacereview.html. Trong file đọc bằng ${workspace.tenField}
//                  (Thymeleaf gọi getTenField(), do @Getter của Lombok tự sinh).
//
// Bên trong hộp lớn này có các hộp nhỏ hơn:
//   VersionViewDTO   (một phiên bản bài)  -> chứa nhiều ContentBlockDTO (các khối thân bài)
//   SourceItemDTO    (một tư liệu)
//   ReviewCommentDTO (một góp ý)          -> chứa các ReviewCommentDTO khác (câu trả lời)
@Getter
@Builder
public class ReviewWorkspaceDTO {
    // ----- Thông tin chung của bài -----
    private Integer articleId;     // mã bài; HTML dùng để ghép URL các form POST, ví dụ @{/department-head/articles/{a}/approve(a=${workspace.articleId})}
    private String code;           // "BT-15" -> breadcrumb, thanh trạng thái, thanh nút đáy: ${workspace.code}
    private String topicTitle;     // tên Topic -> thanh trạng thái: ${workspace.topicTitle}
    private String category;       // chuyên mục -> thanh trạng thái: ${workspace.category}
    private String statusText;     // trạng thái bằng chữ, ví dụ "Chờ Trưởng phòng duyệt" -> ${workspace.statusText}
    // true = bài đang DEPARTMENT_REVIEW và lượt duyệt đang mở là của người đang xem: được góp ý / duyệt / trả bài
    // HTML: mọi khối th:if="${workspace.reviewable}" (khung góp ý, form góp ý chung, nút Xóa, nút Duyệt / Yêu cầu sửa, hộp thoại) chỉ hiện khi true
    private boolean reviewable;
    private String reviewerName;      // tên Trưởng ban -> thanh trên cùng (dhTopbar)
    private String reviewerInitials;  // chữ viết tắt cho avatar Trưởng ban
    private String departmentName;    // tên phòng ban -> thanh trên cùng

    // ----- Dòng tác giả + số liệu (khối ws-byline trong HTML) -----
    private String leadName;          // tên Lead Reporter: ${workspace.leadName}
    private String leadInitials;      // chữ viết tắt cho avatar Lead
    private String supportingNames;   // tên các Supporting, ngăn bằng dấu phẩy; rỗng thì HTML ẩn dòng "Supporting:"
    private String wordCount;         // ví dụ "1.840 từ" (đã định dạng sẵn ở service)
    private int imageCount;           // số tư liệu loại ảnh -> "${workspace.imageCount} ảnh"

    // ----- Các phiên bản -----
    // Version đang duyệt (mới nhất), version liền trước (null nếu mới có v1.0) và toàn bộ version để chọn so sánh
    private VersionViewDTO current;          // bản đang duyệt -> tít, sapo, thân bài: ${workspace.current.title}, ${workspace.current.blocks}...
    private VersionViewDTO previous;         // bản liền trước; null thì HTML ẩn nút so sánh và tab "Vòng trước" (th:if="${workspace.previous != null}")
    private List<VersionViewDTO> versions;   // mọi version -> ô chọn "So với" và khối ẩn #version-data cho JS so sánh

    // ----- Tư liệu -----
    private List<SourceItemDTO> sources;     // khung "Tư liệu đính kèm": th:each="s : ${workspace.sources}"
    // Ảnh đại diện của bài (article_source.is_featured), null nếu chưa chọn
    private SourceItemDTO featuredImage;     // khung ảnh cover dưới sapo: th:if="${workspace.featuredImage != null}"

    // ----- Góp ý -----
    // Góp ý của lượt duyệt trên version đang duyệt / góp ý trên version liền trước
    private List<ReviewCommentDTO> comments;          // tab "Vòng này": th:each="c : ${workspace.comments}"
    private List<ReviewCommentDTO> previousComments;  // tab "Vòng trước": th:each="c : ${workspace.previousComments}"
}
