package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    Optional<Assignment> findFirstByTopic_IdAndAccount_IdAndAssignmentRoleAndStatusNot(
            Integer topicId, Integer accountId, AssignmentRole role, AssignmentStatus status);
    @EntityGraph(attributePaths = "account")
    Optional<Assignment> findFirstByTopic_IdAndAssignmentRoleAndStatusNot(Integer topicId, AssignmentRole role, AssignmentStatus status);
    @EntityGraph(attributePaths = "topic")
    List<Assignment> findByAccount_IdAndAssignmentRoleAndStatusAndTopic_Status(
            Integer accountId, AssignmentRole role, AssignmentStatus status, TopicStatus topicStatus);
}
