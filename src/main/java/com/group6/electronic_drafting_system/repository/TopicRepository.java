package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicRepository extends JpaRepository<Topic, Integer> {
    // Phòng ban của đề tài = phòng ban của người tạo đề tài
    @Query("select t from Topic t join fetch t.creator c "
            + "where t.status = :status and c.department.id = :departmentId order by t.createdAt desc")
    List<Topic> findByStatusAndDepartment(@Param("status") TopicStatus status, @Param("departmentId") Integer departmentId);
}
