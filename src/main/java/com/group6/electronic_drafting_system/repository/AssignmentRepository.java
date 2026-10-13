package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    @EntityGraph(attributePaths = "account")
    List<Assignment> findByTopicAndStatusNot(Topic topic, AssignmentStatus status);
}
