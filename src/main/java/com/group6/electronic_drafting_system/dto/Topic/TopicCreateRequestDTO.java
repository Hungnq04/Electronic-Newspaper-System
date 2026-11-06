package com.group6.electronic_drafting_system.dto.Topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Form "Create Topic Proposal" và "Edit & Resubmit Topic": tiêu đề + tóm tắt.
// Ban phụ trách luôn là ban của phóng viên (service tự lấy), category / priority / status do service tự điền.
@Getter @Setter
public class TopicCreateRequestDTO {
    public static final int TITLE_MAX = 255;
    public static final int SUMMARY_MAX = 4000;

    @NotBlank(message = "Vui lòng nhập tiêu đề đề tài.")
    @Size(max = TITLE_MAX, message = "Tiêu đề tối đa 255 ký tự.")
    private String title;

    @NotBlank(message = "Vui lòng nhập tóm tắt nội dung.")
    @Size(max = SUMMARY_MAX, message = "Tóm tắt tối đa 4000 ký tự.")
    private String summary;
}
