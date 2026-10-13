package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.service.FinalReviewService;
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

// FL-06: Final Review Queue + Final Article Review Workspace của Thư ký tòa soạn
@Controller
@RequestMapping("/secretary/final-reviews")
public class FinalReviewController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FinalReviewController.class);
    private static final String QUEUE = "redirect:/secretary/final-reviews";

    private final FinalReviewService finalReviewService;

    public FinalReviewController(FinalReviewService finalReviewService) {
        this.finalReviewService = finalReviewService;
    }

    // Hiển thị trong layout chung (sidebar + nội dung), xem templates/layout/app.html
    private static String page(Model model, String title, String view) {
        model.addAttribute("pageTitle", title + " | CMS Core");
        model.addAttribute("contentView", view);
        model.addAttribute("contentFragment", "content");
        model.addAttribute("pageCss", List.of("/css/secretary.css"));
        return "layout/app";
    }

    @GetMapping
    public String queue(Principal principal, Model model) {
        model.addAttribute("items", finalReviewService.getQueue(principal.getName()));
        return page(model, "Duyệt cuối", "secretary/final-review-queue");
    }

    @GetMapping("/{articleId}")
    public String workspace(@PathVariable Integer articleId, Principal principal, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("ws", finalReviewService.openWorkspace(articleId, principal.getName()));
            return page(model, "Duyệt cuối bài viết", "secretary/final-review-workspace");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return QUEUE;
        }
    }

    @PostMapping("/{articleId}/approve")
    public String approve(@PathVariable Integer articleId, @RequestParam(required = false) String feedback,
                          Principal principal, RedirectAttributes redirect) {
        try {
            finalReviewService.approve(articleId, principal.getName(), feedback);
            redirect.addFlashAttribute("message", "Đã duyệt cuối bài viết. Bài chuyển sang danh sách chờ xuất bản.");
            return QUEUE;
        } catch (ApplicationException ex) {
            return backToWorkspace(articleId, ex.getErrorCode(), feedback, redirect);
        } catch (Exception ex) {
            log.error("Final approve failed for article {}", articleId, ex);
            return backToWorkspace(articleId, ErrorCode.UNCATEGORIZED_EXCEPTION, feedback, redirect);
        }
    }

    @PostMapping("/{articleId}/request-revision")
    public String requestRevision(@PathVariable Integer articleId, @RequestParam(required = false) String feedback,
                                  Principal principal, RedirectAttributes redirect) {
        try {
            finalReviewService.requestRevision(articleId, principal.getName(), feedback);
            redirect.addFlashAttribute("message", "Đã trả bài về Lead Reporter kèm yêu cầu chỉnh sửa.");
            return QUEUE;
        } catch (ApplicationException ex) {
            return backToWorkspace(articleId, ex.getErrorCode(), feedback, redirect);
        } catch (Exception ex) {
            log.error("Final request-revision failed for article {}", articleId, ex);
            return backToWorkspace(articleId, ErrorCode.UNCATEGORIZED_EXCEPTION, feedback, redirect);
        }
    }

    // Giữ lại nội dung đã nhập để người dùng không phải gõ lại
    private String backToWorkspace(Integer articleId, ErrorCode error, String feedback, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", error.getMessage());
        redirect.addFlashAttribute("feedback", feedback);
        return "redirect:/secretary/final-reviews/" + articleId;
    }
}
