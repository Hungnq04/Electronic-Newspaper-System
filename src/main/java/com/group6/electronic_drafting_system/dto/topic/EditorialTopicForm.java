package com.group6.electronic_drafting_system.dto.topic;

import com.group6.electronic_drafting_system.entity.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

// Form tạo Editorial Topic (UC-05)
@Getter @Setter
public class EditorialTopicForm {
    @NotBlank(message = "Vui lòng nhập tên Topic.") @Size(max = 255, message = "Tên Topic tối đa 255 ký tự.")
    private String title;
    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự.")
    private String description;
    @NotBlank(message = "Vui lòng nhập chuyên mục.") @Size(max = 100, message = "Chuyên mục tối đa 100 ký tự.")
    private String category;
    @Size(max = 4000, message = "Định hướng tối đa 4000 ký tự.")
    private String rationale;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedDate;
    @NotNull(message = "Vui lòng chọn mức ưu tiên.")
    private Priority priority = Priority.NORMAL;
}
