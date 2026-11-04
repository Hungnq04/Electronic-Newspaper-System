package com.group6.electronic_drafting_system.mapper.DepartmentHead;

import com.group6.electronic_drafting_system.dto.DepartmentHead.DraftEntryDTO;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class DraftEntryMapper {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // Cần join fetch article.lead và article.topic trước khi gọi
    public DraftEntryDTO fromArticle(Article article, long sourceCount) {
        Topic topic = article.getTopic();
        OffsetDateTime submittedAt = article.getSubmittedAt() != null ? article.getSubmittedAt() : article.getUpdatedAt();
        return DraftEntryDTO.builder()
                .id(article.getId())
                .kind("article")
                .code("BT-" + article.getId())
                .title(article.getTitle())
                .excerpt(article.getSapo())
                .category(article.getCategory())
                .urgent(topic.getPriority() == Priority.URGENT)
                .authorName(article.getLead().getFullName())
                .authorInitials(initials(article.getLead().getFullName()))
                .submittedAgo(timeAgo(submittedAt))
                .wordCount(String.format(VI, "%,d từ", countWords(article.getContent())))
                .sourceCount(sourceCount + " nguồn")
                .deadlineText(deadlineText(topic.getExpectedDate()))
                .statusText(statusText(article.getStatus()))
                .expectedDate(topic.getExpectedDate())
                .submittedAt(submittedAt)
                .build();
    }

    // Cần join fetch topic.creator trước khi gọi
    public DraftEntryDTO fromTopic(Topic topic) {
        return DraftEntryDTO.builder()
                .id(topic.getId())
                .kind("topic")
                .code("DT-" + topic.getId())
                .title(topic.getTitle())
                .excerpt(topic.getDescription())
                .category(topic.getCategory())
                .urgent(topic.getPriority() == Priority.URGENT)
                .authorName(topic.getCreator().getFullName())
                .authorInitials(initials(topic.getCreator().getFullName()))
                .submittedAgo(timeAgo(topic.getCreatedAt()))
                .deadlineText(deadlineText(topic.getExpectedDate()))
                .statusText(statusText(topic.getStatus()))
                .expectedDate(topic.getExpectedDate())
                .submittedAt(topic.getCreatedAt())
                .build();
    }

    // "Lê Hoàng Sơn" -> "LS"
    public String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        return (parts.length == 1 ? first : first + parts[parts.length - 1].substring(0, 1)).toUpperCase(VI);
    }

    private static int countWords(String content) {
        if (content == null) return 0;
        String text = content.replaceAll("<[^>]*>", " ").trim(); // nội dung có thể là HTML từ trình soạn thảo
        return text.isEmpty() ? 0 : text.split("\\s+").length;
    }

    private static String timeAgo(OffsetDateTime time) {
        if (time == null) return "";
        Duration elapsed = Duration.between(time, OffsetDateTime.now());
        if (elapsed.toMinutes() < 1) return "vừa xong";
        if (elapsed.toMinutes() < 60) return elapsed.toMinutes() + " phút trước";
        if (elapsed.toHours() < 24) return elapsed.toHours() + " giờ trước";
        return time.format(DATE_TIME);
    }

    private static String deadlineText(LocalDate expectedDate) {
        if (expectedDate == null) return null;
        LocalDate today = LocalDate.now();
        String suffix = expectedDate.isBefore(today) ? " (đã quá hạn)" : expectedDate.isEqual(today) ? " (hôm nay)" : "";
        return "Dự kiến đăng: " + expectedDate.format(DATE) + suffix;
    }

    private static String statusText(ArticleStatus status) {
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case DEPARTMENT_REVIEW -> "Chờ Trưởng ban thẩm định";
            case REVISION_REQUIRED -> "Đã yêu cầu chỉnh sửa";
            case DEPARTMENT_APPROVED -> "Đã chuyển Ban Thư ký tòa soạn";
            case APPROVED -> "Đã phê duyệt";
            case SCHEDULED -> "Đã lên lịch đăng";
            case PUBLISHED -> "Đã xuất bản";
        };
    }

    private static String statusText(TopicStatus status) {
        return switch (status) {
            case DEPARTMENT_REVIEW -> "Chờ Trưởng ban thẩm định đề tài";
            case SECRETARY_REVIEW -> "Chờ Ban Thư ký tòa soạn duyệt";
            case ACTIVE -> "Đang triển khai";
            case REJECTED -> "Đã từ chối";
        };
    }
}
