package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    @EntityGraph(attributePaths = "account")
    List<Assignment> findByTopicAndStatusNot(Topic topic, AssignmentStatus status);

    @EntityGraph(attributePaths = {"account", "account.department", "topic"})
    List<Assignment> findByTopicInAndStatusNot(Collection<Topic> topics, AssignmentStatus status);

    // Số Topic mỗi Trưởng phòng đang phụ trách: mỗi phần tử là [accountId, count]
    @Query("select a.account.id, count(a) from Assignment a where a.assignmentRole = :role and a.status <> :removed group by a.account.id")
    List<Object[]> countActiveByRole(@Param("role") AssignmentRole role, @Param("removed") AssignmentStatus removed);

    // Trưởng phòng đang phụ trách Topic (tối đa một - BR-38)
    @EntityGraph(attributePaths = "account")
    Optional<Assignment> findFirstByTopicAndAssignmentRoleAndStatusNot(Topic topic, AssignmentRole role, AssignmentStatus status);
}
