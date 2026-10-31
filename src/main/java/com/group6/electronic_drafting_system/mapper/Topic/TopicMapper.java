package com.group6.electronic_drafting_system.mapper.Topic;

import com.group6.electronic_drafting_system.dto.Topic.DepartmentOptionDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicHistoryEntryDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicListItemDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicReviewItemDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.TopicFeedback;
import com.group6.electronic_drafting_system.entity.TopicReview;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.TopicFeedbackType;
import com.group6.electronic_drafting_system.entity.enums.TopicReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TopicMapper {
    private static final String DEPARTMENT_PREFIX = "Phòng ";
    private static final int CATEGORY_MAX = 100;

    public TopicListItemDTO toListItem(Topic topic) {
        Department department = topic.getDepartment();
        return new TopicListItemDTO(topic.getId(), codeOf(topic), topic.getTitle(), topic.getDescription(),
                department == null ? null : department.getDepartmentName(),
                topic.getStatus(), topic.isAwaitingRevision(), topic.getCreatedAt());
    }

    // FL-01 Iter2: một dòng trên màn duyệt của Trưởng ban / Thư ký (cần creator + department đã nạp)
    public TopicReviewItemDTO toReviewItem(Topic topic, int revisionRounds) {
        Department department = topic.getDepartment();
        return new TopicReviewItemDTO(topic.getId(), codeOf(topic), topic.getTitle(), topic.getDescription(),
                department == null ? null : department.getDepartmentName(), topic.getCreator().getFullName(),
                topic.getCreatedAt(), topic.getUpdatedAt(), revisionRounds, topic.getStatus(), topic.isAwaitingRevision());
    }

    // FL-01 Iter2: lịch sử của đề tài = các vòng góp ý (topic_feedback) + kết quả duyệt từng cấp (topic_review),
    // xếp theo thời gian, cũ nhất trước
    public List<TopicHistoryEntryDTO> toHistory(List<TopicFeedback> feedbacks, List<TopicReview> reviews) {
        List<TopicHistoryEntryDTO> history = new ArrayList<>();
        for (TopicFeedback f : feedbacks) {
            boolean changeRequest = f.getType() == TopicFeedbackType.CHANGE_REQUEST;
            history.add(new TopicHistoryEntryDTO(changeRequest ? "Trưởng ban yêu cầu sửa" : "Từ chối",
                    changeRequest ? "revision" : "rejected", f.getAuthor().getFullName(), f.getCreatedAt(), f.getContent()));
        }
        for (TopicReview r : reviews) {
            String level = r.getReviewStage() == ReviewStage.DEPARTMENT ? "Trưởng ban" : "Thư ký";
            boolean approved = r.getDecision() == TopicReviewDecision.APPROVED;
            history.add(new TopicHistoryEntryDTO(level + (approved ? " đã duyệt" : " từ chối"),
                    approved ? "approved" : "rejected", r.getReviewer().getFullName(), r.getReviewedAt(), r.getFeedback()));
        }
        history.sort(Comparator.comparing(TopicHistoryEntryDTO::createdAt));
        return history;
    }

    public DepartmentOptionDTO toOption(Department department) {
        return new DepartmentOptionDTO(department.getId(), department.getDepartmentName());
    }

    // Topic mới vừa gửi: vào thẳng hàng chờ Trưởng ban (không có DRAFT).
    // category theo quy ước dữ liệu cũ = tên phòng bỏ "Phòng " ('Phòng Kinh tế' -> 'Kinh tế');
    // priority mặc định NORMAL, rationale / expected_date để trống.
    public Topic toNewTopic(TopicCreateRequestDTO request, Department department, Account reporter) {
        Topic topic = new Topic();
        topic.setCreator(reporter);
        topic.setTitle(request.getTitle().trim());
        topic.setDescription(request.getSummary().trim());
        topic.setDepartment(department);
        topic.setCategory(categoryOf(department));
        topic.setPriority(Priority.NORMAL);
        topic.setStatus(TopicStatus.DEPARTMENT_REVIEW);
        topic.setAwaitingRevision(false);
        return topic;
    }

    public static String categoryOf(Department department) {
        String name = department.getDepartmentName().trim();
        String category = name.startsWith(DEPARTMENT_PREFIX) ? name.substring(DEPARTMENT_PREFIX.length()).trim() : name;
        return category.length() > CATEGORY_MAX ? category.substring(0, CATEGORY_MAX) : category;
    }

    // TP-<năm tạo>-<id 4 chữ số>, ví dụ TP-2026-0012
    public static String codeOf(Topic topic) {
        int year = topic.getCreatedAt() == null ? 0 : topic.getCreatedAt().getYear();
        return "TP-%d-%04d".formatted(year, topic.getId());
    }
}
