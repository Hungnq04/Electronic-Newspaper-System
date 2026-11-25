package com.group6.electronic_drafting_system.dto.Reporter;

// Tiến độ nhiệm vụ của phóng viên. Việc suy ra tiến độ nằm ở ReporterTaskServiceImpl.stageOf().
// Mỗi hằng chỉ chứa dữ liệu cố định:
//   label    : chữ hiển thị
//   group    : nhóm cho tab lọc và cột Kanban (todo / doing / review / done)
//   progress : % cho thanh tiến độ
//   open     : true = việc còn đang làm (chưa nộp / chưa xong)
public enum TaskStage {
    NOT_STARTED("Chưa bắt đầu", "todo", 0, true),
    IN_PROGRESS("Đang thực hiện", "doing", 40, true),
    REVISION("Cần chỉnh sửa", "doing", 60, true),
    IN_REVIEW("Chờ duyệt", "review", 85, false),
    DONE("Hoàn thành", "done", 100, false);

    private final String label;
    private final String group;
    private final int progress;
    private final boolean open;

    TaskStage(String label, String group, int progress, boolean open) {
        this.label = label;
        this.group = group;
        this.progress = progress;
        this.open = open;
    }

    public String getLabel() { return label; }
    public String getGroup() { return group; }
    public int getProgress() { return progress; }
    public boolean isOpen() { return open; }
}
