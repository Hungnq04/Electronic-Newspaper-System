package com.group6.electronic_drafting_system.mapper.Reporter;

import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDTO;
import com.group6.electronic_drafting_system.dto.Reporter.ReporterTaskDetailDTO;
import com.group6.electronic_drafting_system.dto.Reporter.TaskStage;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Source;
import com.group6.electronic_drafting_system.entity.Topic;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// Chỉ chép dữ liệu từ entity sang DTO. Các giá trị cần tính (nhãn, deadline, tiến độ...)
// do ReporterTaskServiceImpl tính rồi truyền vào.
@Component
public class ReporterTaskMapper {

    // article = null khi nhiệm vụ chưa có bài viết
    public ReporterTaskDTO toTaskDTO(Assignment assignment, Article article, String priorityLabel, String roleLabel, boolean lead,
                                     String assignedByInitials, String deadlineText, String deadlineBucket,
                                     int imageCount, int documentCount, int otherSourceCount, int sourceCount,
                                     TaskStage stage, boolean urgent, boolean overdue, boolean canCreateArticle,
                                     String writeHref, String newArticleHref, String writeLabel) {
        Topic topic = assignment.getTopic();
        return new ReporterTaskDTO(assignment.getId(), topic.getId(), topic.getTitle(), topic.getCategory(),
                topic.getPriority(), priorityLabel, assignment.getAssignmentRole(), roleLabel, lead,
                assignment.getAssignedBy().getFullName(), assignedByInitials, assignment.getAssignedAt(), assignment.getDeadline(),
                deadlineText, deadlineBucket, assignment.getRequirements(),
                article == null ? null : article.getId(), article == null ? null : article.getTitle(),
                article == null ? null : article.getStatus(), article == null ? null : article.getUpdatedAt(),
                imageCount, documentCount, otherSourceCount, sourceCount,
                stage, urgent, overdue, canCreateArticle,
                writeHref, newArticleHref, writeLabel);
    }

    public ReporterTaskDetailDTO.Member toMember(Assignment assignment, String initials, String roleLabel, boolean me) {
        Account account = assignment.getAccount();
        return new ReporterTaskDetailDTO.Member(account.getFullName(), initials, assignment.getAssignmentRole(), roleLabel, me);
    }

    public ReporterTaskDetailDTO.SourceItem toSourceItem(Source source, String typeLabel, String note, String addedByName,
                                                         String origin, boolean image, boolean video) {
        return new ReporterTaskDetailDTO.SourceItem(source.getId(), source.getTitle(), source.getSourceType(), typeLabel,
                source.getFileUrl(), source.getDescription(), note, addedByName, source.getUploadedAt(), origin, image, video);
    }

    // GIỮ LẠI vì code của thành viên khác (TopicServiceImpl, TopicReviewServiceImpl) đang gọi.
    // Code của mình dùng bản private initials() trong từng service impl.
    // "Nguyễn Thu Hà" -> "TH": lấy chữ cái đầu của hai từ cuối
    public static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] words = fullName.trim().split("\\s+");
        return Arrays.stream(words, Math.max(0, words.length - 2), words.length)
                .map(w -> w.substring(0, 1))
                .collect(Collectors.joining())
                .toUpperCase(Locale.ROOT);
    }
}
