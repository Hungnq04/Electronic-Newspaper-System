package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    // Mọi comment (gốc + trả lời) của các lượt duyệt trên một version, kèm người viết và lượt duyệt
    @Query("select c from Comment c join fetch c.creator join fetch c.review r "
            + "where r.version.id = :versionId order by c.createdAt, c.id")
    List<Comment> findByVersion(@Param("versionId") Integer versionId);

    @Query("select c from Comment c join fetch c.review r join fetch r.version v where c.id = :id")
    Optional<Comment> findWithReview(@Param("id") Integer id);
}
