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

// FL-07 Iter3: Scheduled Publication List + Scheduled Publication Detail (kèm pop-up Hủy lịch & trả về Lead).
// SecurityConfig đã chỉ cho Thư ký vào /secretary/**; service kiểm tra quyền thêm một lần nữa.
@Controller
@RequestMapping("/secretary/scheduled-publications")
public class ScheduledPublicationController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScheduledPublicationController.class);
    private static final String LIST = "redirect:/secretary/scheduled-publications";

    private final PublicationService publicationService;

    public ScheduledPublicationController(PublicationService publicationService) {
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

    // Màn #67 Scheduled Publication List
    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("items", publicationService.getScheduledArticles(principal.getName()));
        return page(model, "Đã hẹn giờ đăng", "secretary/scheduled-list");
    }

    // Màn #68 Scheduled Publication Detail
    @GetMapping("/{articleId}")
    public String detail(@PathVariable Integer articleId, Principal principal, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("pub", publicationService.getScheduledDetail(articleId, principal.getName()));
            return page(model, "Lịch đăng bài viết", "secretary/scheduled-detail");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return LIST;
        }
    }

    // Pop-up #69 Cancel & Return to Lead Form -> bấm "Hủy lịch và trả bài"
    @PostMapping("/{articleId}/cancel")
    public String cancelAndReturn(@PathVariable Integer articleId, @RequestParam(required = false) String reason,
                                  Principal principal, RedirectAttributes redirect) {
        try {
            publicationService.cancelAndReturn(articleId, principal.getName(), reason);
            redirect.addFlashAttribute("message", "Đã hủy lịch đăng và trả bài về Lead Reporter.");
            return LIST;
        } catch (ApplicationException ex) {
            return backToDetail(articleId, ex.getErrorCode(), reason, redirect);
        } catch (Exception ex) {
            log.error("Cancel schedule failed for article {}", articleId, ex);
            return backToDetail(articleId, ErrorCode.UNCATEGORIZED_EXCEPTION, reason, redirect);
        }
    }

    // Quay lại màn chi tiết kèm thông báo lỗi; giữ lý do đã gõ để mở lại pop-up
    private String backToDetail(Integer articleId, ErrorCode error, String reason, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", error.getMessage());
        redirect.addFlashAttribute("cancelReason", reason == null ? "" : reason);
        return "redirect:/secretary/scheduled-publications/" + articleId;
    }
}
