package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.dto.Article.ArticleCreateRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleDraftRequestDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleEditorDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleMediaUploadResponseDTO;
import com.group6.electronic_drafting_system.dto.Article.ArticleSaveResponseDTO;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.service.ArticleService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/articles")
public class ArticleController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ArticleController.class);
    private static final String ARTICLES_PATH = "/articles";
    private static final String TASKS_PATH = "/reporter/tasks";
    private static final String FONTS = "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Lora:ital,wght@0,400;0,600;0,700;1,400&family=Merriweather:ital,wght@0,400;0,700;1,400&family=Noto+Serif:ital,wght@0,400;0,700;1,400&family=Playfair+Display:wght@600;700&family=Roboto:ital,wght@0,400;0,700;1,400&display=swap";
    private static final String LUCIDE = "https://unpkg.com/lucide@0.460.0/dist/umd/lucide.min.js";
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        model.addAttribute("page", articleService.getListPage(currentEmail(auth)));
        model.addAttribute("pageTitle", "Bài viết của tôi – Phóng viên");
        model.addAttribute("contentView", "articles/list");
        model.addAttribute("contentFragment", "articleListContent");
        model.addAttribute("pageCss", List.of("/reporter/css/reporter-tasks.css"));
        model.addAttribute("activePath", ARTICLES_PATH);
        return "layout/app";
    }

    // Trang viết bài mới: bắt buộc ?assignmentId= của nhiệm vụ mình được giao với vai trò viết chính
    @GetMapping("/new")
    public String newArticle(@ModelAttribute ArticleCreateRequestDTO form, Authentication auth, Model model, RedirectAttributes redirect) {
        try {
            return editorPage(model, articleService.getNewArticleForm(form, currentEmail(auth)), TASKS_PATH);
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return "redirect:" + TASKS_PATH;
        }
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
        // Hiển thị lại form với nội dung đã nhập để không mất bài; nhiệm vụ không hợp lệ thì quay về danh sách nhiệm vụ
        try {
            model.addAttribute("changeSummary", form.getChangeSummary());
            return editorPage(model, articleService.getNewArticleForm(form, email), TASKS_PATH);
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return "redirect:" + TASKS_PATH;
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Authentication auth, Model model, RedirectAttributes redirect) {
        try {
            return editorPage(model, articleService.getEditor(id, currentEmail(auth)), ARTICLES_PATH);
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return "redirect:" + ARTICLES_PATH;
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
            // Giữ lại ghi chú vừa gõ để hiện lại trong ô "Ghi chú" (bản nháp đã được service giữ lại)
            redirect.addFlashAttribute("changeSummary", request.getChangeSummary());
        } catch (Exception ex) {
            log.error("Submit failed for article {}", id, ex);
            redirect.addFlashAttribute("error", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }
        return "redirect:/articles/" + id + "/edit";
    }

    // Khôi phục version cũ vào bản đang soạn (UC-24). Nút "Khôi phục bản này" trong cửa sổ chi tiết phiên bản.
    // Service kiểm tra: đúng Lead đang hoạt động, bài đang DRAFT / REVISION_REQUIRED, version thuộc bài này.
    @PostMapping("/{id}/versions/{versionId}/restore")
    public String restoreVersion(@PathVariable Integer id, @PathVariable Integer versionId, Authentication auth, RedirectAttributes redirect) {
        try {
            String label = articleService.restoreVersion(id, versionId, currentEmail(auth));
            redirect.addFlashAttribute("message", "Đã khôi phục nội dung phiên bản " + label
                    + " vào bản đang soạn. Lịch sử phiên bản giữ nguyên; phiên bản mới sẽ được tạo khi bạn nộp lại.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Restore version {} failed for article {}", versionId, id, ex);
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

    // Chèn ảnh / video lấy từ tư liệu đề tài (topic_source) vào bài
    @PostMapping("/{id}/topic-media")
    @ResponseBody
    public ResponseEntity<ArticleMediaUploadResponseDTO> attachTopicMedia(@PathVariable Integer id, @RequestParam Integer sourceId, Authentication auth) {
        try {
            return ResponseEntity.ok(articleService.attachTopicMedia(id, sourceId, currentEmail(auth)));
        } catch (ApplicationException ex) {
            return ResponseEntity.status(ex.getErrorCode().getStatusCode()).body(ArticleMediaUploadResponseDTO.error(ex.getErrorCode().getMessage()));
        } catch (Exception ex) {
            log.error("Attach topic media {} failed for article {}", sourceId, id, ex);
            return ResponseEntity.internalServerError().body(ArticleMediaUploadResponseDTO.error(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()));
        }
    }

    // Tải ảnh / video từ máy lên Cloudinary (multipart: file, caption)
    @PostMapping(value = "/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<ArticleMediaUploadResponseDTO> uploadMedia(@PathVariable Integer id, @RequestParam(required = false) MultipartFile file,
                                                                     @RequestParam(required = false) String caption, Authentication auth) {
        try {
            return ResponseEntity.ok(articleService.uploadMedia(id, file, caption, currentEmail(auth)));
        } catch (ApplicationException ex) {
            return ResponseEntity.status(ex.getErrorCode().getStatusCode()).body(ArticleMediaUploadResponseDTO.error(ex.getErrorCode().getMessage()));
        } catch (Exception ex) {
            log.error("Upload media failed for article {}", id, ex);
            return ResponseEntity.internalServerError().body(ArticleMediaUploadResponseDTO.error(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()));
        }
    }

    // Trình soạn thảo nằm trong layout chung (sidebar theo quyền + topbar phóng viên)
    private static String editorPage(Model model, ArticleEditorDTO article, String activePath) {
        model.addAttribute("article", article);
        model.addAttribute("pageTitle", (article.isNew() ? "Viết bài mới" : article.title()) + " – Phóng viên");
        model.addAttribute("contentView", "articles/editor");
        model.addAttribute("contentFragment", "articleEditorContent");
        // article-feedback: phần góp ý của Trưởng ban / Thư ký khi bài bị trả về (file riêng, nạp sau editor)
        model.addAttribute("pageCss", List.of(FONTS, "/reporter/css/reporter-tasks.css", "/css/editor.css", "/reporter/css/article-feedback.css"));
        model.addAttribute("pageJs", List.of(LUCIDE, "/js/editor.js", "/reporter/js/article-feedback.js"));
        model.addAttribute("activePath", activePath);
        return "layout/app";
    }

    // Đăng nhập form: username là email; đăng nhập Google: lấy thuộc tính email
    private static String currentEmail(Authentication auth) {
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof OAuth2User google && google.getAttribute("email") instanceof String email) return email;
        return auth.getName();
    }
}
