package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.dto.Article.ArticleCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleDraftRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleSaveResponseDTO;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/articles")
public class ArticleController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ArticleController.class);
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        String email = currentEmail(auth);
        model.addAttribute("articles", articleService.getMyArticles(email));
        model.addAttribute("waitingTopics", articleService.getTopicsWaitingForArticle(email));
        return "articles/list";
    }

    // Trang viết bài mới; ?topicId= để chọn sẵn đề tài
    @GetMapping("/new")
    public String newArticle(@ModelAttribute ArticleCreateRequestDTO form, Authentication auth, Model model) {
        return renderNewArticle(form, currentEmail(auth), model);
    }

    // action = draft: tạo bài ở trạng thái nháp; action = submit: tạo và nộp luôn cho Trưởng ban
    @PostMapping
    public String create(@ModelAttribute ArticleCreateRequestDTO form, @RequestParam(defaultValue = "draft") String action,
                         Authentication auth, Model model, RedirectAttributes redirect) {
        String email = currentEmail(auth);
        boolean submit = "submit".equals(action);
        try {
            Integer id = articleService.create(form, submit, email);
            redirect.addFlashAttribute("message", submit ? "Đã tạo và nộp bài cho Trưởng ban duyệt." : "Đã tạo bài viết. Bản nháp sẽ được tự động lưu.");
            return "redirect:/articles/" + id + "/edit";
        } catch (ApplicationException ex) {
            model.addAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Create article failed", ex);
            model.addAttribute("error", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }
        // Hiển thị lại form với nội dung đã nhập để không mất bài
        return renderNewArticle(form, email, model);
    }

    private String renderNewArticle(ArticleCreateRequestDTO form, String email, Model model) {
        model.addAttribute("article", articleService.getNewArticleForm(form, email));
        model.addAttribute("topics", articleService.getTopicsWaitingForArticle(email));
        model.addAttribute("changeSummary", form.getChangeSummary());
        return "articles/editor";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Authentication auth, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("article", articleService.getEditor(id, currentEmail(auth)));
            return "articles/editor";
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return "redirect:/articles";
        }
    }

    // Lưu nháp (tự động hoặc bấm nút): chỉ cập nhật bảng article, không tạo version
    @PostMapping("/{id}/draft")
    @ResponseBody
    public ResponseEntity<ArticleSaveResponseDTO> saveDraft(@PathVariable Integer id, @RequestBody ArticleDraftRequestDTO request, Authentication auth) {
        try {
            return ResponseEntity.ok(new ArticleSaveResponseDTO(true, "Đã lưu nháp", articleService.saveDraft(id, request, currentEmail(auth))));
        } catch (ApplicationException ex) {
            return ResponseEntity.status(ex.getErrorCode().getStatusCode()).body(new ArticleSaveResponseDTO(false, ex.getErrorCode().getMessage(), null));
        } catch (Exception ex) {
            log.error("Save draft failed for article {}", id, ex);
            return ResponseEntity.internalServerError().body(new ArticleSaveResponseDTO(false, ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage(), null));
        }
    }

    // Nộp bài: lưu bản đang soạn, chụp article_version mới và chuyển sang chờ Trưởng ban duyệt
    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Integer id, @ModelAttribute ArticleDraftRequestDTO request, Authentication auth, RedirectAttributes redirect) {
        try {
            articleService.submit(id, request, currentEmail(auth));
            redirect.addFlashAttribute("message", "Đã nộp bài. Bản thảo được chuyển tới Trưởng ban duyệt.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Submit failed for article {}", id, ex);
            redirect.addFlashAttribute("error", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }
        return "redirect:/articles/" + id + "/edit";
    }

    @PostMapping("/{id}/featured")
    @ResponseBody
    public ResponseEntity<ArticleSaveResponseDTO> setFeatured(@PathVariable Integer id, @RequestParam Integer articleSourceId, Authentication auth) {
        try {
            articleService.setFeaturedImage(id, articleSourceId, currentEmail(auth));
            return ResponseEntity.ok(new ArticleSaveResponseDTO(true, "Đã đổi ảnh đại diện", null));
        } catch (ApplicationException ex) {
            return ResponseEntity.status(ex.getErrorCode().getStatusCode()).body(new ArticleSaveResponseDTO(false, ex.getErrorCode().getMessage(), null));
        }
    }

    // Đăng nhập form: username là email; đăng nhập Google: lấy thuộc tính email
    private static String currentEmail(Authentication auth) {
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof OAuth2User google && google.getAttribute("email") instanceof String email) return email;
        return auth.getName();
    }
}
