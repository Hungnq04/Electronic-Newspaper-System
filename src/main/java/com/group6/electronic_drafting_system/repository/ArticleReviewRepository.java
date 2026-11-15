package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.ArticleReview;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; 
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleVersion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ArticleReviewRepository extends JpaRepository<ArticleReview, Integer> {
    // Lượt đang mở (chưa quyết định) của một cấp trên một version - DB chỉ cho phép tối đa một
    @EntityGraph(attributePaths = "reviewer")
    Optional<ArticleReview> findByVersionAndReviewStageAndDecisionIsNull(ArticleVersion version, ReviewStage stage);

    boolean existsByVersionAndReviewStageAndDecision(ArticleVersion version, ReviewStage stage, ArticleReviewDecision decision);

    @EntityGraph(attributePaths = {"reviewer", "version"})
    List<ArticleReview> findByVersionIn(Collection<ArticleVersion> versions);

    @EntityGraph(attributePaths = {"reviewer", "version"})
    List<ArticleReview> findByVersionArticleAndDecisionIsNotNullOrderByReviewedAtAscIdAsc(Article article);

    // Lượt duyệt gần nhất ĐÃ có quyết định của một bài (bỏ qua lượt đang duyệt dở, phóng viên chưa được thấy)
    @EntityGraph(attributePaths = "reviewer")
    Optional<ArticleReview> findFirstByVersion_Article_IdAndDecisionIsNotNullOrderByIdDesc(Integer articleId);

    @Query("select count(r) from ArticleReview r where r.reviewStage = :stage and r.decision = :decision "
            + "and r.reviewedAt >= :from and r.reviewer.id = :reviewerId")
    long countDecisionsSince(@Param("stage") ReviewStage stage, @Param("decision") ArticleReviewDecision decision,
                             @Param("from") OffsetDateTime from, @Param("reviewerId") Integer reviewerId);

    // Màn "Lịch sử duyệt" (UC-35): mọi lượt duyệt ĐÃ CHỐT (decision khác NULL) của MỘT người ở MỘT cấp, mới nhất trước.
    // "join fetch" = lấy luôn version, bài và đề tài trong CÙNG câu SQL, để khi hiển thị không phải hỏi DB thêm từng dòng.
    // Gọi từ DepartmentHeadServiceImpl.getReviewHistory(): reviewerId = Trưởng ban đang đăng nhập, stage = DEPARTMENT.
    @Query("select r from ArticleReview r join fetch r.version v join fetch v.article a join fetch a.topic "
            + "where r.reviewer.id = :reviewerId and r.reviewStage = :stage and r.decision is not null "
            + "order by r.reviewedAt desc, r.id desc")
    List<ArticleReview> findDecidedByReviewer(@Param("reviewerId") Integer reviewerId, @Param("stage") ReviewStage stage);

    // Lượt duyệt đang mở (decision = NULL) của một cấp trên một version; DB chỉ cho phép tối đa 1 dòng (uq_review_open)
    @Query("select r from ArticleReview r join fetch r.reviewer where r.version.id = :versionId "
            + "and r.reviewStage = :stage and r.decision is null")
    Optional<ArticleReview> findOpenReview(@Param("versionId") Integer versionId, @Param("stage") ReviewStage stage);
}
