package com.group6.electronic_drafting_system.dto.Topic;

import java.util.List;

// Topic Feedback Viewer của phóng viên: đề tài + lịch sử góp ý / kết quả duyệt.
// canEdit: Trưởng ban đang yêu cầu sửa (DEPARTMENT_REVIEW + awaiting_revision) thì hiện nút "Sửa đề tài".
public record TopicFeedbackViewDTO(TopicListItemDTO topic, List<TopicHistoryEntryDTO> history, boolean canEdit) {

    // Góp ý yêu cầu sửa gần nhất (hiện phía trên form sửa)
    public TopicHistoryEntryDTO latestChangeRequest() {
        TopicHistoryEntryDTO latest = null;
        for (TopicHistoryEntryDTO entry : history) {
            if ("revision".equals(entry.tone())) latest = entry;
        }
        return latest;
    }
}
