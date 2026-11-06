package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.entity.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group6.electronic_drafting_system.dto.DepartmentHead.DashboardDTO;
import com.group6.electronic_drafting_system.service.DepartmentHeadService;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.ArticleReviewRepository;
import com.group6.electronic_drafting_system.repository.TopicRepository;
import com.group6.electronic_drafting_system.dto.DepartmentHead.PendingArticleDTO;
import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.enums.ArticleReviewDecision;
import com.group6.electronic_drafting_system.entity.enums.ReviewStage;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DepartmentHeadServiceImpl implements DepartmentHeadService {
    // Implement methods defined in the DepartmentHeadService interface here
    private final AccountRepository accounts;
    private final ArticleRepository articles;
    private final ArticleReviewRepository articlesReview;
    private final TopicRepository topics;

    public DepartmentHeadServiceImpl(AccountRepository accountRepository, ArticleRepository articleRepository, ArticleReviewRepository articleReviewRepository, TopicRepository topicRepository) {
        this.accounts = accountRepository;
        this.articles = articleRepository;
        this.articlesReview = articleReviewRepository;
        this.topics = topicRepository;
    }

    @Override
    public DashboardDTO getDashboard(String email) {
         // 00:00 hôm nay
        OffsetDateTime startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        Account head = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED));
        // Lấy số lượng bài được duyệt trong ngày của trưởng phòng
        long approvedTodayCount = articlesReview.countDecisionsSince(ReviewStage.DEPARTMENT,ArticleReviewDecision.APPROVED , startOfToday,  head.getId());
        long pendingTopicCount =0;
        if(head.getDepartment() != null){
            pendingTopicCount = topics.findByStatusAndDepartment(TopicStatus.DEPARTMENT_REVIEW, head.getDepartment().getId()).size();

        }
        // Lấy danh sách bài đang chờ duyệt của trưởng phòng
        // Lấy số lượng bài trả về chỉnh sửa của trưởng phòng
        List<Article> articleList = new ArrayList<>();
        long revisionCount = 0;
        if(head.getDepartment() != null){
            articleList = articles.findByStatusForDepartment(ArticleStatus.DEPARTMENT_REVIEW,head.getDepartment().getId(), head.getId());
            revisionCount =articles.findByStatusForDepartment(ArticleStatus.REVISION_REQUIRED,head.getDepartment().getId(), head.getId()).size();
        }
        List<PendingArticleDTO> pendingArticles = new ArrayList<>();
        for (Article article : articleList) {
            PendingArticleDTO pendingArticleDTO = PendingArticleDTO.builder()
                    .articleId(article.getId())
                    .title(article.getTitle())
                    .build();
            pendingArticles.add(pendingArticleDTO);
        }
        return DashboardDTO.builder()
                .email(head.getEmail())
                .username(head.getUsername())
                .fullName(head.getFullName())
                .rC(revisionCount)
                .pendingArticlesCount(pendingArticles.size())
                .pendingArticles(pendingArticles)
                .chapthuanngayhomnay(approvedTodayCount)
                .detaidangchoduyet(pendingTopicCount)
                .build();
    }

}