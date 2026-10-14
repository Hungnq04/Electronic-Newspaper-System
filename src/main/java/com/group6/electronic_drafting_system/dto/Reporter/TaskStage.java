package com.group6.electronic_drafting_system.dto.Reporter;

// Tiến độ nhiệm vụ của phóng viên, suy ra từ trạng thái assignment + bài viết của đề tài.
// group dùng cho tab lọc và cột Kanban: todo / doing / review / done; progress (%) cho thanh tiến độ
public enum TaskStage {
    NOT_STARTED("Chưa bắt đầu", "todo", 0),
    IN_PROGRESS("Đang thực hiện", "doing", 40),
    REVISION("Cần chỉnh sửa", "doing", 60),
    IN_REVIEW("Chờ duyệt", "review", 85),
    DONE("Hoàn thành", "done", 100);

    private final String label;
    private final String group;
    private final int progress;

    TaskStage(String label, String group, int progress) {
        this.label = label;
        this.group = group;
        this.progress = progress;
    }

    public String getLabel() { return label; }
    public String getGroup() { return group; }
    public int getProgress() { return progress; }
    public boolean isOpen() { return this == NOT_STARTED || this == IN_PROGRESS || this == REVISION; }
}
