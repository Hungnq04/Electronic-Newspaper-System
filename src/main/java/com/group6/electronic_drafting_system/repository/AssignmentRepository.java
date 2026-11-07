package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Assignment;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.AssignmentRole;
import com.group6.electronic_drafting_system.entity.enums.AssignmentStatus;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    // Admin: việc đang làm của một tài khoản (status = ASSIGNED, đúng điều kiện của rule R23)
    @EntityGraph(attributePaths = "topic")
    List<Assignment> findByAccount_IdAndStatusOrderByAssignedAtDesc(Integer accountId, AssignmentStatus status);

    @EntityGraph(attributePaths = "account")
    List<Assignment> findByTopicAndStatusNot(Topic topic, AssignmentStatus status);

    Optional<Assignment> findFirstByTopic_IdAndAccount_IdAndAssignmentRoleAndStatusNot(
            Integer topicId, Integer accountId, AssignmentRole role, AssignmentStatus status);
    @EntityGraph(attributePaths = "account")
    Optional<Assignment> findFirstByTopic_IdAndAssignmentRoleAndStatusNot(Integer topicId, AssignmentRole role, AssignmentStatus status);
    @EntityGraph(attributePaths = "topic")
    List<Assignment> findByAccount_IdAndAssignmentRoleAndStatusAndTopic_Status(
            Integer accountId, AssignmentRole role, AssignmentStatus status, TopicStatus topicStatus);
    // Nhiệm vụ của phóng viên (viết chính / hỗ trợ) trên các đề tài đã duyệt
    @EntityGraph(attributePaths = {"topic", "assignedBy"})
    List<Assignment> findByAccount_IdAndAssignmentRoleInAndStatusNotAndTopic_Status(
            Integer accountId, Collection<AssignmentRole> roles, AssignmentStatus status, TopicStatus topicStatus);
    @EntityGraph(attributePaths = {"topic", "assignedBy", "account"})
    Optional<Assignment> findWithDetailsById(Integer id);
    // Đề tài mà một người đang phụ trách với một vai trò (ví dụ Trưởng phòng), kèm người đề xuất đề tài
    @EntityGraph(attributePaths = {"topic", "topic.creator"})
    List<Assignment> findByAccount_IdAndAssignmentRoleAndStatusNotAndTopic_Status(
            Integer accountId, AssignmentRole role, AssignmentStatus status, TopicStatus topicStatus);
    // Lịch sử nhân sự: các dòng đã REMOVED của một đề tài, mới nhất trước
    @EntityGraph(attributePaths = {"account", "removedBy"})
    List<Assignment> findByTopic_IdAndStatusOrderByRemovedAtDesc(Integer topicId, AssignmentStatus status);
    // Thành viên hiện tại của nhiều đề tài cùng lúc, tránh truy vấn theo từng dòng
    @EntityGraph(attributePaths = "account")
    List<Assignment> findByTopic_IdInAndStatusNot(Collection<Integer> topicIds, AssignmentStatus status);

    // "Ai đang phụ trách hiện tại": luôn bỏ qua dòng REMOVED (CLAUDE.md mục 5)
    @Query("select count(s) > 0 from Assignment s where s.topic.id = :topicId and s.account.id = :accountId "
            + "and s.assignmentRole = :role "
            + "and s.status <> com.group6.electronic_drafting_system.entity.enums.AssignmentStatus.REMOVED")
    boolean isActiveOnTopic(@Param("topicId") Integer topicId, @Param("accountId") Integer accountId,
                            @Param("role") AssignmentRole role);

    @Query("select s from Assignment s join fetch s.account where s.topic.id = :topicId and s.assignmentRole = :role "
            + "and s.status <> com.group6.electronic_drafting_system.entity.enums.AssignmentStatus.REMOVED")
    List<Assignment> findActiveByTopicAndRole(@Param("topicId") Integer topicId, @Param("role") AssignmentRole role);
    @EntityGraph(attributePaths = {"account", "account.department", "topic"})
    List<Assignment> findByTopicInAndStatusNot(Collection<Topic> topics, AssignmentStatus status);

    // Số Topic mỗi Trưởng phòng đang phụ trách: mỗi phần tử là [accountId, count]
    @Query("select a.account.id, count(a) from Assignment a where a.assignmentRole = :role and a.status <> :removed group by a.account.id")
    List<Object[]> countActiveByRole(@Param("role") AssignmentRole role, @Param("removed") AssignmentStatus removed);

    // Trưởng phòng đang phụ trách Topic (tối đa một - BR-38)
    @EntityGraph(attributePaths = "account")
    Optional<Assignment> findFirstByTopicAndAssignmentRoleAndStatusNot(Topic topic, AssignmentRole role, AssignmentStatus status);
}
