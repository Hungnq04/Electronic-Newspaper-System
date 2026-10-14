package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.TopicSource;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicSourceRepository extends JpaRepository<TopicSource, Integer> {
    @EntityGraph(attributePaths = {"source", "source.addedBy"})
    List<TopicSource> findByTopic_IdOrderByAttachedAtAsc(Integer topicId);
    @EntityGraph(attributePaths = "source")
    List<TopicSource> findByTopic_IdIn(Collection<Integer> topicIds);
    @EntityGraph(attributePaths = {"source", "source.addedBy"})
    Optional<TopicSource> findByTopic_IdAndSource_Id(Integer topicId, Integer sourceId);
}
