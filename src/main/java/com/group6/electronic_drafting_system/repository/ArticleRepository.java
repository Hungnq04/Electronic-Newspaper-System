package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.ArticleSource;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Integer> {
    // Bài chờ Trưởng phòng X = bài thuộc Topic mà X đang là DEPARTMENT_HEAD (assignment chưa REMOVED), theo CLAUDE.md mục 6.3 / rule R09
    String HEAD_OF_TOPIC = "exists (select 1 from Assignment s where s.topic = a.topic and s.account.id = :headId "
            + "and s.assignmentRole = com.group6.electronic_drafting_system.entity.enums.AssignmentRole.DEPARTMENT_HEAD "
            + "and s.status <> com.group6.electronic_drafting_system.entity.enums.AssignmentStatus.REMOVED)";

    @Query("select a from Article a join fetch a.lead join fetch a.topic "
            + "where a.status = :status and " + HEAD_OF_TOPIC + " order by a.submittedAt desc")
    List<Article> findByStatusForDepartmentHead(@Param("status") ArticleStatus status, @Param("headId") Integer headId);

    @Query("select count(a) from Article a where a.status = :status and " + HEAD_OF_TOPIC)
    long countByStatusForDepartmentHead(@Param("status") ArticleStatus status, @Param("headId") Integer headId);

    // Đếm nguồn của nhiều bài trong 1 query (tránh N+1 khi gọi article.getSources().size()); mỗi dòng: [articleId, count]
    @Query("select s.article.id, count(s) from ArticleSource s where s.article.id in :articleIds group by s.article.id")
    List<Object[]> countSourcesByArticleIds(@Param("articleIds") Collection<Integer> articleIds);

    @Query("select a from Article a join fetch a.lead join fetch a.topic where a.id = :id")
    Optional<Article> findWithLeadAndTopic(@Param("id") Integer id);

    @Query("select s from ArticleSource s join fetch s.source where s.article.id = :articleId "
            + "order by s.featured desc, s.displayOrder, s.id")
    List<ArticleSource> findSources(@Param("articleId") Integer articleId);
    @EntityGraph(attributePaths = {"topic", "lead"})
    List<Article> findByStatusOrderBySubmittedAtAsc(ArticleStatus status);

    @EntityGraph(attributePaths = {"topic", "lead"})
    Optional<Article> findWithTopicAndLeadById(Integer id);

    // Khóa dòng khi đổi trạng thái để hai người không cùng quyết định một bài
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Article a where a.id = :id")
    Optional<Article> findByIdForUpdate(@Param("id") Integer id);

    @EntityGraph(attributePaths = "topic")
    List<Article> findByLead_IdOrderByUpdatedAtDesc(Integer leadId);

    @EntityGraph(attributePaths = "lead")
    List<Article> findByTopic_IdIn(Collection<Integer> topicIds);
}
