package com.group6.electronic_drafting_system.dto.Topic;

import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.util.List;

// Dữ liệu trang Topic Proposal List. tabs: "Tất cả" + 4 status, kèm số đếm trên toàn bộ Topic được xem
// (không phụ thuộc tab đang chọn); topics: danh sách đã lọc theo tab.
public record TopicProposalListDTO(String fullName, String roleLabel, String initials,
                                   List<StatusTab> tabs, List<TopicListItemDTO> topics) {

    // status == null là tab "Tất cả"
    public record StatusTab(TopicStatus status, String label, long count, boolean active) {
        public String filterValue() { return status == null ? null : status.name(); }
        public String key() { return status == null ? "all" : TopicListItemDTO.keyOf(status); }
    }

    public long countOf(String key) {
        return tabs.stream().filter(t -> t.key().equals(key)).mapToLong(StatusTab::count).findFirst().orElse(0);
    }
}
