package com.group6.electronic_drafting_system.dto.Topic;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

// Form giao / giao lại Topic cho Trưởng phòng (UC-06, UC-07)
@Getter @Setter
public class TopicAssignForm {
    @NotNull(message = "Vui lòng chọn Trưởng phòng.")
    private Integer departmentHeadId;
    // Giờ địa phương từ ô datetime-local; bắt buộc (nhóm chốt 16/11). Giờ phải ở tương lai: kiểm tra ở service (ASSIGNMENT_DEADLINE_PAST)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @NotNull(message = "Vui lòng chọn deadline.")
    private LocalDateTime deadline;
    @Size(max = 4000, message = "Yêu cầu tối đa 4000 ký tự.")
    private String requirements;
}
