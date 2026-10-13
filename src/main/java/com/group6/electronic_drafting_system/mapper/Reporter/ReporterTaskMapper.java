package com.group6.electronic_drafting_system.mapper.Reporter;

import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDTO;
import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDetailDTO;
import com.group6.electronic_drafting_system.dto.Reporter.TaskStage;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Source;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import com.group6.electronic_drafting_system.entity.enums.SourceType;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ReporterTaskMapper {
    // Nhiệm vụ còn hạn dưới mốc này được xếp vào "Cần nộp gấp"
    private static final Duration URGENT_WINDOW = Duration.ofHours(24);

    // sourceTypes: loại của các tư liệu gắn với đề tài / bài viết, dùng để đếm ảnh, tài liệu
    public ReporterTaskDTO toTaskDTO(Assignment assignment, Article article, Collection<SourceType> sourceTypes, OffsetDateTime now) {
        Topic topic = assignment.getTopic();
        TaskStage stage = stageOf(assignment, article);
        OffsetDateTime deadline = assignment.getDeadline();
        boolean overdue = stage.isOpen() && deadline != null && deadline.isBefore(now);
        boolean urgent = stage.isOpen() && (topic.getPriority() == Priority.URGENT
                || (deadline != null && deadline.isBefore(now.plus(URGENT_WINDOW))));
        long images = sourceTypes.stream().filter(t -> t == SourceType.IMAGE).count();
        long documents = sourceTypes.stream().filter(t -> t == SourceType.DOCUMENT || t == SourceType.INTERNAL_DOC).count();

        return new ReporterTaskDTO(assignment.getId(), topic.getId(), topic.getTitle(), topic.getCategory(),
                topic.getPriority(), priorityLabel(topic.getPriority()), assignment.getAssignmentRole(), roleLabel(assignment.getAssignmentRole()),
                assignment.getAssignedBy().getFullName(), initials(assignment.getAssignedBy().getFullName()), assignment.getAssignedAt(), deadline,
                deadlineText(stage, deadline, article == null ? null : article.getSubmittedAt(), now), deadlineBucket(deadline, now),
                assignment.getRequirements(),
                article == null ? null : article.getId(), article == null ? null : article.getTitle(),
                article == null ? null : article.getStatus(), article == null ? null : article.getUpdatedAt(),
                (int) images, (int) documents, (int) (sourceTypes.size() - images - documents),
                stage, urgent, overdue,
                assignment.getAssignmentRole() == AssignmentRole.LEAD && assignment.getStatus() == AssignmentStatus.ASSIGNED
                        && topic.getStatus() == TopicStatus.ACTIVE);
    }

    public ReporterTaskDetailDTO.Member toMember(Assignment assignment, Integer myId) {
        Account account = assignment.getAccount();
        return new ReporterTaskDetailDTO.Member(account.getFullName(), initials(account.getFullName()), assignment.getAssignmentRole(),
                roleLabel(assignment.getAssignmentRole()), account.getId().equals(myId));
    }

    public ReporterTaskDetailDTO.SourceItem toSourceItem(Source source, String note, String origin) {
        return new ReporterTaskDetailDTO.SourceItem(source.getId(), source.getTitle(), source.getSourceType(), sourceTypeLabel(source.getSourceType()),
                source.getFileUrl(), source.getDescription(), note,
                source.getAddedBy() == null ? null : source.getAddedBy().getFullName(), source.getUploadedAt(), origin);
    }

    // Đã nộp / đã duyệt thì coi như xong phần viết; assignment COMPLETED là hoàn thành hẳn
    static TaskStage stageOf(Assignment assignment, Article article) {
        if (assignment.getStatus() == AssignmentStatus.COMPLETED) return TaskStage.DONE;
        if (article == null) return TaskStage.NOT_STARTED;
        return switch (article.getStatus()) {
            case DRAFT -> TaskStage.IN_PROGRESS;
            case REVISION_REQUIRED -> TaskStage.REVISION;
            case DEPARTMENT_REVIEW, DEPARTMENT_APPROVED -> TaskStage.IN_REVIEW;
            case APPROVED, SCHEDULED, PUBLISHED -> TaskStage.DONE;
        };
    }

    static String deadlineText(TaskStage stage, OffsetDateTime deadline, OffsetDateTime submittedAt, OffsetDateTime now) {
        if (deadline == null) return null;
        if (!stage.isOpen()) {
            if (submittedAt == null) return null;
            return submittedAt.isAfter(deadline) ? "Nộp trễ hạn" : "Nộp đúng hạn";
        }
        Duration left = Duration.between(now, deadline);
        return (left.isNegative() ? "Quá hạn " : "Còn ") + formatDuration(left.abs());
    }

    static String formatDuration(Duration d) {
        if (d.toDays() > 0) return d.toDays() + " ngày" + (d.toHoursPart() > 0 ? " " + d.toHoursPart() + " giờ" : "");
        if (d.toHours() > 0) return d.toHours() + " giờ " + d.toMinutesPart() + " phút";
        return Math.max(d.toMinutes(), 1) + " phút";
    }

    static String deadlineBucket(OffsetDateTime deadline, OffsetDateTime now) {
        if (deadline == null) return "none";
        if (deadline.isBefore(now)) return "overdue";
        LocalDate day = deadline.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = now.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        if (day.equals(today)) return "today";
        return day.isBefore(today.plusDays(7)) ? "week" : "later";
    }

    public static String priorityLabel(Priority priority) {
        return switch (priority) {
            case URGENT -> "Hỏa tốc";
            case HIGH -> "Ưu tiên cao";
            case NORMAL -> "Thường";
            case LOW -> "Thấp";
        };
    }

    public static String roleLabel(AssignmentRole role) {
        return switch (role) {
            case LEAD -> "Viết chính";
            case SUPPORTING -> "Hỗ trợ tác nghiệp";
            case DEPARTMENT_HEAD -> "Trưởng ban phụ trách";
        };
    }

    public static String sourceTypeLabel(SourceType type) {
        return switch (type) {
            case IMAGE -> "Ảnh";
            case DOCUMENT -> "Tài liệu";
            case WEBSITE -> "Website";
            case INTERVIEW -> "Phỏng vấn";
            case INTERNAL_DOC -> "Nội bộ";
        };
    }

    // "Nguyễn Thu Hà" -> "TH": lấy chữ cái đầu của hai từ cuối
    public static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] words = fullName.trim().split("\s+");
        return Arrays.stream(words, Math.max(0, words.length - 2), words.length)
                .map(w -> w.substring(0, 1))
                .collect(Collectors.joining())
                .toUpperCase(Locale.ROOT);
    }
}
