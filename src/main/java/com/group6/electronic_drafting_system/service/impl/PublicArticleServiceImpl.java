package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.entity.PublicArticle;
import com.group6.electronic_drafting_system.repository.PublicArticleRepository;
import com.group6.electronic_drafting_system.service.PublicArticleService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class PublicArticleServiceImpl implements PublicArticleService {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("articleId"));

    private final PublicArticleRepository publicArticles;

    @Override public List<PublicArticle> getLatest() {
        return publicArticles.findTop7ByOrderByPublishedAtDescArticleIdDesc();
    }

    @Override public Page<PublicArticle> search(String keyword, String category, int page) {
        return publicArticles.search(escapeLike(blankToNull(keyword)), blankToNull(category),
                PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE, NEWEST_FIRST));
    }

    @Override public Optional<PublicArticle> findPublished(int articleId) {
        return publicArticles.findById(articleId);
    }

    @Override public List<PublicArticle> getRelated(PublicArticle article) {
        return publicArticles.findTop3ByCategoryAndArticleIdNotOrderByPublishedAtDescArticleIdDesc(article.getCategory(), article.getArticleId());
    }

    @Override public List<String> getCategories() {
        return publicArticles.findCategories();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String escapeLike(String value) {
        return value == null ? null : value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
