package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Comment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    @EntityGraph(attributePaths = {"creator", "review", "review.version"})
    List<Comment> findByReviewVersionArticleOrderByCreatedAtAscIdAsc(Article article);

    // Mọi comment (gốc + trả lời) của các lượt duyệt trên một version, kèm người viết và lượt duyệt
    @Query("select c from Comment c join fetch c.creator join fetch c.review r "
            + "where r.version.id = :versionId order by c.createdAt, c.id")
    List<Comment> findByVersion(@Param("versionId") Integer versionId);

    @Query("select c from Comment c join fetch c.review r join fetch r.version v where c.id = :id")
    Optional<Comment> findWithReview(@Param("id") Integer id);

    // Màn "Lịch sử duyệt" (UC-35): đếm số góp ý GỐC (không tính câu trả lời) của nhiều lượt duyệt trong 1 câu SQL.
    // Kết quả: mỗi dòng là [mã lượt duyệt, số góp ý], ví dụ [[31, 3], [35, 1]]. Lượt nào không có góp ý thì không có dòng.
    @Query("select c.review.id, count(c) from Comment c where c.review.id in :reviewIds and c.parent is null group by c.review.id")
    List<Object[]> countRootCommentsByReviewIds(@Param("reviewIds") Collection<Integer> reviewIds);
}
