package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Integer> {
    @EntityGraph(attributePaths = {"topic", "lead"})
    List<Article> findByStatusOrderBySubmittedAtAsc(ArticleStatus status);

    @EntityGraph(attributePaths = {"topic", "lead"})
    Optional<Article> findWithTopicAndLeadById(Integer id);

    // Số Article của từng Topic: mỗi phần tử là [topicId, count]
    @Query("select a.topic.id, count(a) from Article a where a.topic.id in :topicIds group by a.topic.id")
    List<Object[]> countByTopicIds(@Param("topicIds") Collection<Integer> topicIds);

    // Khóa dòng khi đổi trạng thái để hai người không cùng quyết định một bài
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Article a where a.id = :id")
    Optional<Article> findByIdForUpdate(@Param("id") Integer id);
}
