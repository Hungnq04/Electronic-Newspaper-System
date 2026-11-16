package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// Toàn bộ dữ liệu màn "Lịch sử phiên bản" (UC-23, FL-05) của Trưởng phòng.
// TẠO Ở ĐÂU: DepartmentHeadReviewServiceImpl.getVersionHistory()
// HTML ĐỌC:  ${versionHistory...} trong department_head/version-history.html
@Getter
@Builder
public class VersionHistoryPageDTO {
    private Integer articleId;          // ghép URL quay lại màn thẩm định / mở so sánh
    private String articleCode;         // "BT-15"
    private String articleTitle;        // tít bài
    private String topicTitle;
    private String category;
    private String leadName;
    private String statusText;          // trạng thái bài hiện tại, chữ tiếng Việt
    // Các phiên bản, MỚI NHẤT TRƯỚC. Dùng lại VersionViewDTO của màn thẩm định
    // (đã có nhãn, ngày nộp, ghi chú, tít, sapo, các đoạn) + 2 ô reviewNotes, commentCount.
    private List<VersionViewDTO> versions;
    // 3 ô cho thanh trên cùng (fragment dhTopbar)
    private String displayName;
    private String departmentName;
    private String userInitials;
    
    // ===== 4 ô chỉ dùng cho màn "Chi tiết phiên bản"; màn Lịch sử phiên bản để trống =====
    // @Setter: service điền vào SAU khi đã tạo xong hộp (getVersionDetail gọi lại getVersionHistory rồi điền thêm).
    @Setter private VersionViewDTO selectedVersion;            // phiên bản đang xem (tít, sapo, ảnh, lượt duyệt...)
    @Setter private List<ReviewCommentDTO> selectedComments;   // góp ý trên phiên bản đang xem
    @Setter private VersionViewDTO olderVersion;               // phiên bản liền TRƯỚC (cũ hơn); null nếu đang xem v1.0
    @Setter private VersionViewDTO newerVersion;               // phiên bản liền SAU (mới hơn); null nếu đang xem bản mới nhất
}