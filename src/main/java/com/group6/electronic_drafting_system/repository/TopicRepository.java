package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Integer> {
    // Phóng viên: Topic do chính mình đề xuất
    @EntityGraph(attributePaths = "department")
    List<Topic> findByCreator_IdOrderByCreatedAtDescIdDesc(Integer creatorId);

    // Trưởng ban: Topic thuộc ban của mình
    @EntityGraph(attributePaths = "department")
    List<Topic> findByDepartment_IdOrderByCreatedAtDescIdDesc(Integer departmentId);

    // Thư ký tòa soạn: Topic đang chờ duyệt cuối
    @EntityGraph(attributePaths = "department")
    List<Topic> findByStatusOrderByCreatedAtDescIdDesc(TopicStatus status);
}
