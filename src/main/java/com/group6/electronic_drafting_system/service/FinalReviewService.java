package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.review.FinalReviewQueueItemDTO;
import com.group6.electronic_drafting_system.dto.review.FinalReviewWorkspaceDTO;
import java.util.List;

// Duyệt cuối Article của Thư ký tòa soạn (FL-06: UC-26 Final Review, UC-27 Request Final Revision)
public interface FinalReviewService {
    List<FinalReviewQueueItemDTO> getQueue(String secretaryEmail);

    // Mở bài: tạo Review cấp SECRETARY nếu chưa có lượt nào đang mở (PRD 4.7)
    FinalReviewWorkspaceDTO openWorkspace(Integer articleId, String secretaryEmail);

    void approve(Integer articleId, String secretaryEmail, String feedback);

    void requestRevision(Integer articleId, String secretaryEmail, String feedback);
}
