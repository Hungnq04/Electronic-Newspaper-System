package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    // "Ai đang phụ trách hiện tại": luôn bỏ qua dòng REMOVED (CLAUDE.md mục 5)
    @Query("select count(s) > 0 from Assignment s where s.topic.id = :topicId and s.account.id = :accountId "
            + "and s.assignmentRole = :role "
            + "and s.status <> com.group6.electronic_drafting_system.entity.enums.AssignmentStatus.REMOVED")
    boolean isActiveOnTopic(@Param("topicId") Integer topicId, @Param("accountId") Integer accountId,
                            @Param("role") AssignmentRole role);

    @Query("select s from Assignment s join fetch s.account where s.topic.id = :topicId and s.assignmentRole = :role "
            + "and s.status <> com.group6.electronic_drafting_system.entity.enums.AssignmentStatus.REMOVED")
    List<Assignment> findActiveByTopicAndRole(@Param("topicId") Integer topicId, @Param("role") AssignmentRole role);
}
