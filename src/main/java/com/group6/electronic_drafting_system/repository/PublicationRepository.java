package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Publication;
import com.group6.electronic_drafting_system.entity.enums.PublicationStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// FL-07: bảng publication (mỗi bài tối đa một dòng)
public interface PublicationRepository extends JpaRepository<Publication, Integer> {

    // Lịch đăng của một bài (nếu có)
    Optional<Publication> findByArticle_Id(Integer articleId);

    // Job tự đăng: mã các bài có lịch đã đến giờ mà chưa đăng, giờ sớm nhất đăng trước
    @Query("select p.article.id from Publication p where p.status = :status and p.scheduledAt <= :now order by p.scheduledAt")
    List<Integer> findDueArticleIds(@Param("status") PublicationStatus status, @Param("now") OffsetDateTime now);
}
