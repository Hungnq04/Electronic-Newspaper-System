package com.group6.electronic_drafting_system.dto.DepartmentHead;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

// Dữ liệu form "Giao việc" gửi lên (POST). Mỗi người có nhiệm vụ + deadline riêng (CLAUDE.md mục 4.1 B).
// Deadline nhận dạng chuỗi từ <input type="datetime-local">, ví dụ "2026-10-30T17:00" (giờ Việt Nam), service tự đổi sang OffsetDateTime.
// Phần Supporting: supporterIds là các phóng viên được tick; hai Map tra theo account_id,
// ví dụ input name="supporterDeadlines[12]" là deadline của phóng viên có account_id = 12.
@Getter @Setter
public class TopicAssignmentFormDTO {
    private Integer leadAccountId;          // Chỉ dùng khi đề tài chưa có Lead (đề tài Thư ký giao xuống)
    private String leadDeadline;
    private String leadRequirements;
    private List<Integer> supporterIds = new ArrayList<>();
    private Map<Integer, String> supporterDeadlines = new HashMap<>();
    private Map<Integer, String> supporterRequirements = new HashMap<>();

    // Form lúc mới mở trang: điền sẵn nhiệm vụ / deadline hiện có của Lead (nếu có)
    public static TopicAssignmentFormDTO prefill(String leadDeadline, String leadRequirements) {
        TopicAssignmentFormDTO form = new TopicAssignmentFormDTO();
        form.setLeadDeadline(leadDeadline);
        form.setLeadRequirements(leadRequirements);
        return form;
    }

    public boolean isSupporter(Integer accountId) { return supporterIds.contains(accountId); }
}
