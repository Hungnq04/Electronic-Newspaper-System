package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.PublicArticle;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// View chỉ đọc: không gọi save / delete.
public interface PublicArticleRepository extends JpaRepository<PublicArticle, Integer> {
    List<PublicArticle> findTop7ByOrderByPublishedAtDescArticleIdDesc();

    List<PublicArticle> findTop3ByCategoryAndArticleIdNotOrderByPublishedAtDescArticleIdDesc(String category, Integer articleId);

    // keyword đã được escape ký tự đại diện của LIKE (\, %, _) ở service
    @Query("""
            select p from PublicArticle p
            where (:category is null or p.category = :category)
              and (:keyword is null
                   or lower(p.title) like lower(concat('%', :keyword, '%')) escape '\\'
                   or lower(p.sapo) like lower(concat('%', :keyword, '%')) escape '\\')
            """)
    Page<PublicArticle> search(@Param("keyword") String keyword, @Param("category") String category, Pageable pageable);

    @Query("select p.category from PublicArticle p group by p.category order by p.category")
    List<String> findCategories();
}
