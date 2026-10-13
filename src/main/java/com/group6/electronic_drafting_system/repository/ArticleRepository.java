package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Article;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Integer> {
    @EntityGraph(attributePaths = {"topic", "lead", "lead.department"})
    Optional<Article> findWithDetailsById(Integer id);
    @EntityGraph(attributePaths = "topic")
    List<Article> findByLead_IdOrderByUpdatedAtDesc(Integer leadId);
    boolean existsByTopic_IdAndLead_Id(Integer topicId, Integer leadId);
}
