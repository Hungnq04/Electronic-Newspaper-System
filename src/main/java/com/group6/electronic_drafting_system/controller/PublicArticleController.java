package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.entity.PublicArticle;
import com.group6.electronic_drafting_system.service.PublicArticleService;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

// FL-07 trang công khai (UC-33, UC-34): độc giả không cần đăng nhập.
@Controller @RequiredArgsConstructor
public class PublicArticleController {
    private final PublicArticleService publicArticleService;

    @ModelAttribute("currentUser") String currentUser(Principal principal) {
        return principal == null ? null : principal.getName();
    }

    @ModelAttribute("categories") List<String> categories() {
        return publicArticleService.getCategories();
    }

    @GetMapping("/")
    public String home(Model model) {
        List<PublicArticle> latest = publicArticleService.getLatest();
        model.addAttribute("featured", latest.isEmpty() ? null : latest.get(0));
        model.addAttribute("latest", latest.size() > 1 ? latest.subList(1, latest.size()) : List.of());
        return "public/home";
    }

    @GetMapping("/news")
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "1") int page, Model model) {
        Page<PublicArticle> result = publicArticleService.search(q, category, page);
        model.addAttribute("result", result);
        model.addAttribute("q", q == null ? "" : q.strip());
        model.addAttribute("category", category == null || category.isBlank() ? null : category.strip());
        return "public/article-list";
    }

    @GetMapping("/news/{id:\\d+}")
    public String detail(@PathVariable int id, Model model, HttpServletResponse response) {
        PublicArticle article = publicArticleService.findPublished(id).orElse(null);
        if (article == null) {
            // Bài chưa đăng (DRAFT, APPROVED, SCHEDULED...) hay không tồn tại đều trả cùng một trang 404
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "public/not-found";
        }
        model.addAttribute("article", article);
        model.addAttribute("related", publicArticleService.getRelated(article));
        return "public/article-detail";
    }
}
