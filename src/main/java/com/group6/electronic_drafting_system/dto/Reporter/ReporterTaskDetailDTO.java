package com.group6.electronic_drafting_system.dto.Reporter;

import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// Trang chi tiết một nhiệm vụ: thông tin đề tài, nhóm thực hiện, tư liệu và bài viết (nếu đã có).
public record ReporterTaskDetailDTO(ReporterTaskDTO task, String fullName, String departmentName, String initials,
                                    String description, String rationale, LocalDate expectedDate, String topicCreatorName,
                                    String articleSapo, OffsetDateTime articleSubmittedAt,
                                    List<Member> team, List<SourceItem> sources) {

    public record Member(String name, String initials, AssignmentRole role, String roleLabel, boolean me) {
    }

    // origin: tư liệu gắn với đề tài hay với bài viết
    public record SourceItem(Integer id, String title, SourceType type, String typeLabel, String fileUrl,
                             String description, String note, String addedByName, OffsetDateTime uploadedAt, String origin) {
        public boolean image() { return type == SourceType.IMAGE && fileUrl != null && !fileUrl.isBlank(); }
    }
}
