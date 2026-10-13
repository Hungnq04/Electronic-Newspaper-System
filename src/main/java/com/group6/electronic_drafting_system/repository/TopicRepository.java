package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicRepository extends JpaRepository<Topic, Integer> {
    @EntityGraph(attributePaths = {"creator", "creator.role"})
    List<Topic> findByStatusOrderByCreatedAtDesc(TopicStatus status);

    @EntityGraph(attributePaths = {"creator", "creator.role"})
    Optional<Topic> findWithCreatorById(Integer id);

    // Khóa dòng khi giao / giao lại để hai Thư ký không cùng giao một Topic
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Topic t where t.id = :id")
    Optional<Topic> findByIdForUpdate(@Param("id") Integer id);

    // Gợi ý chuyên mục khi tạo Topic
    @Query("select distinct t.category from Topic t order by t.category")
    List<String> findDistinctCategories();
}
