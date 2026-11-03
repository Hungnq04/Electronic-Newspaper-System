package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Publication.ApprovedArticleItemDTO;
import com.group6.electronic_drafting_system.dto.Publication.PublicationDetailDTO;
import com.group6.electronic_drafting_system.dto.Publication.ScheduledArticleItemDTO;
import java.util.List;

// FL-07: Thư ký xuất bản bài đã duyệt cuối
// Iter2: UC-29 đăng ngay, UC-30 hẹn giờ, UC-32 tự đăng khi đến giờ
// Iter3: UC-28 trả bài về Lead, UC-31 hủy lịch & trả bài về Lead
public interface PublicationService {
    // Hẹn giờ phải sau thời điểm hiện tại ít nhất bấy nhiêu phút
    int MIN_SCHEDULE_MINUTES = 5;

    // Approved Article List: các bài APPROVED
    List<ApprovedArticleItemDTO> getApprovedArticles(String secretaryEmail);

    // Publication Detail: nội dung bài + thông tin duyệt
    PublicationDetailDTO getDetail(Integer articleId, String secretaryEmail);

    // UC-29: đăng ngay. APPROVED -> PUBLISHED
    void publishNow(Integer articleId, String secretaryEmail);

    // UC-30: hẹn giờ. APPROVED -> SCHEDULED. scheduledAtText dạng "2026-11-01T08:30" (giờ Việt Nam, từ ô chọn ngày giờ)
    void schedule(Integer articleId, String secretaryEmail, String scheduledAtText);

    // UC-32 (job): mã các bài có lịch đã đến giờ
    List<Integer> findDueScheduledArticleIds();

    // UC-32 (job): đăng một bài đến giờ, chỉ khi bài VẪN CÒN SCHEDULED. Trả về true nếu đã đăng
    boolean publishScheduled(Integer articleId);

    // ---------------------------------------------------------------- Iter3

    // UC-28: trả bài APPROVED về Lead, lý do bắt buộc. APPROVED -> REVISION_REQUIRED
    void returnToLead(Integer articleId, String secretaryEmail, String reason);

    // Scheduled Publication List: các bài SCHEDULED, giờ đăng sớm nhất đứng đầu
    List<ScheduledArticleItemDTO> getScheduledArticles(String secretaryEmail);

    // Scheduled Publication Detail: chỉ mở được bài đang SCHEDULED
    PublicationDetailDTO getScheduledDetail(Integer articleId, String secretaryEmail);

    // UC-31: hủy lịch đăng (xóa publication) và trả bài về Lead, trước giờ đăng, lý do bắt buộc. SCHEDULED -> REVISION_REQUIRED
    void cancelAndReturn(Integer articleId, String secretaryEmail, String reason);
}
