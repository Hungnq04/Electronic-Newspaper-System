package com.group6.electronic_drafting_system.mapper.Topic;

import com.group6.electronic_drafting_system.dto.Topic.DepartmentOptionDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Topic.TopicListItemDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Department;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.Priority;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
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
    private static String codeOf(Topic topic) {
        int year = topic.getCreatedAt() == null ? 0 : topic.getCreatedAt().getYear();
        return "TP-%d-%04d".formatted(year, topic.getId());
    }
}
