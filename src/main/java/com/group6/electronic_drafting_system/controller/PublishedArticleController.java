package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.service.PublicationService;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// FL-07, thêm sau góp ý hội đồng: menu "Đã xuất bản" của Thư ký tòa soạn
//   - danh sách bài đang hiển thị trên trang công khai (PUBLISHED)
//   - chi tiết một bài đã đăng + pop-up Gỡ bài (bài PUBLISHED -> trả về Lead)
// Nút "Gỡ bài xuống" trên trang đọc bài công khai (/news/{id}) trỏ tới màn chi tiết ở đây.
// SecurityConfig đã chỉ cho Thư ký vào /secretary/**; service kiểm tra quyền thêm một lần nữa.
@Controller
@RequestMapping("/secretary/published-articles")
public class PublishedArticleController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PublishedArticleController.class);
    private static final String LIST = "redirect:/secretary/published-articles";

    private final PublicationService publicationService;

    public PublishedArticleController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    // Hiển thị trong layout chung (sidebar + nội dung), giống PublicationController
    private static String page(Model model, String title, String view) {
        model.addAttribute("pageTitle", title + " | CMS Core");
        model.addAttribute("contentView", view);
        model.addAttribute("contentFragment", "content");
        model.addAttribute("pageCss", List.of("/css/secretary.css", "/css/publication.css"));
        return "layout/app";
    }

    // Danh sách bài đã xuất bản
    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("items", publicationService.getPublishedArticles(principal.getName()));
        return page(model, "Đã xuất bản", "secretary/published-list");
    }

    // Chi tiết một bài đã xuất bản (có nút Gỡ bài)
    @GetMapping("/{articleId}")
    public String detail(@PathVariable Integer articleId, Principal principal, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("pub", publicationService.getPublishedDetail(articleId, principal.getName()));
            return page(model, "Bài đã xuất bản", "secretary/published-detail");
        } catch (ApplicationException ex) {
            // Bài không tồn tại hoặc đã bị gỡ: quay về danh sách kèm thông báo
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return LIST;
        }
    }

    // Pop-up "Gỡ bài khỏi trang công khai" -> bấm "Gỡ bài và trả về Lead"
    @PostMapping("/{articleId}/unpublish")
    public String unpublish(@PathVariable Integer articleId, @RequestParam(required = false) String reason,
                            Principal principal, RedirectAttributes redirect) {
        try {
            publicationService.unpublish(articleId, principal.getName(), reason);
            redirect.addFlashAttribute("message", "Đã gỡ bài khỏi trang công khai và trả về Lead Reporter.");
            return LIST;
        } catch (ApplicationException ex) {
            return backToDetail(articleId, ex.getErrorCode(), reason, redirect);
        } catch (Exception ex) {
            log.error("Unpublish failed for article {}", articleId, ex);
            return backToDetail(articleId, ErrorCode.UNCATEGORIZED_EXCEPTION, reason, redirect);
        }
    }

    // Quay lại màn chi tiết kèm thông báo lỗi; giữ lý do đã gõ để mở lại pop-up, người dùng không phải gõ lại
    private String backToDetail(Integer articleId, ErrorCode error, String reason, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", error.getMessage());
        redirect.addFlashAttribute("unpublishReason", reason == null ? "" : reason);
        return "redirect:/secretary/published-articles/" + articleId;
    }
}
