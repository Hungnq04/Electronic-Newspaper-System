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

// FL-07 Iter2: Approved Article List + Publication Detail (kèm pop-up Đăng ngay / Hẹn giờ) của Thư ký tòa soạn.
// Iter3: thêm pop-up Trả bài về Lead trên Publication Detail.
// SecurityConfig đã chỉ cho Thư ký vào /secretary/**; service kiểm tra quyền thêm một lần nữa.
@Controller
@RequestMapping("/secretary/publications")
public class PublicationController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PublicationController.class);
    private static final String LIST = "redirect:/secretary/publications";

    private final PublicationService publicationService;

    public PublicationController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    // Hiển thị trong layout chung (sidebar + nội dung), xem templates/layout/app.html
    private static String page(Model model, String title, String view) {
        model.addAttribute("pageTitle", title + " | CMS Core");
        model.addAttribute("contentView", view);
        model.addAttribute("contentFragment", "content");
        model.addAttribute("pageCss", List.of("/css/secretary.css", "/css/publication.css"));
        return "layout/app";
    }

    // Màn #61 Approved Article List
    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("items", publicationService.getApprovedArticles(principal.getName()));
        return page(model, "Chờ xuất bản", "secretary/publication-list");
    }

    // Màn #62 Publication Detail
    @GetMapping("/{articleId}")
    public String detail(@PathVariable Integer articleId, Principal principal, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("pub", publicationService.getDetail(articleId, principal.getName()));
            return page(model, "Xuất bản bài viết", "secretary/publication-detail");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return LIST;
        }
    }

    // Pop-up #64 Publish Now Confirmation -> bấm "Xác nhận đăng ngay"
    @PostMapping("/{articleId}/publish")
    public String publishNow(@PathVariable Integer articleId, Principal principal, RedirectAttributes redirect) {
        try {
            publicationService.publishNow(articleId, principal.getName());
            redirect.addFlashAttribute("message", "Đã xuất bản bài viết. Độc giả có thể đọc ngay trên trang công khai.");
            return LIST;
        } catch (ApplicationException ex) {
            return backToDetail(articleId, ex.getErrorCode(), null, redirect);
        } catch (Exception ex) {
            log.error("Publish now failed for article {}", articleId, ex);
            return backToDetail(articleId, ErrorCode.UNCATEGORIZED_EXCEPTION, null, redirect);
        }
    }

    // Pop-up #65 Schedule Publication Form -> #66 Schedule Confirmation -> bấm "Xác nhận hẹn giờ"
    @PostMapping("/{articleId}/schedule")
    public String schedule(@PathVariable Integer articleId, @RequestParam(required = false) String scheduledAt,
                           Principal principal, RedirectAttributes redirect) {
        try {
            publicationService.schedule(articleId, principal.getName(), scheduledAt);
            redirect.addFlashAttribute("message", "Đã hẹn giờ đăng bài. Hệ thống sẽ tự xuất bản khi đến giờ.");
            return LIST;
        } catch (ApplicationException ex) {
            return backToDetail(articleId, ex.getErrorCode(), scheduledAt, redirect);
        } catch (Exception ex) {
            log.error("Schedule failed for article {}", articleId, ex);
            return backToDetail(articleId, ErrorCode.UNCATEGORIZED_EXCEPTION, scheduledAt, redirect);
        }
    }

    // Iter3 - Pop-up #63 Return to Lead Form -> bấm "Trả bài về Lead Reporter"
    @PostMapping("/{articleId}/return")
    public String returnToLead(@PathVariable Integer articleId, @RequestParam(required = false) String reason,
                               Principal principal, RedirectAttributes redirect) {
        try {
            publicationService.returnToLead(articleId, principal.getName(), reason);
            redirect.addFlashAttribute("message", "Đã trả bài về Lead Reporter. Bài chuyển sang trạng thái cần chỉnh sửa.");
            return LIST;
        } catch (ApplicationException ex) {
            // Giữ lại lý do đã gõ để mở lại pop-up, người dùng không phải gõ lại
            redirect.addFlashAttribute("returnReason", reason == null ? "" : reason);
            return backToDetail(articleId, ex.getErrorCode(), null, redirect);
        } catch (Exception ex) {
            log.error("Return to lead failed for article {}", articleId, ex);
            return backToDetail(articleId, ErrorCode.UNCATEGORIZED_EXCEPTION, null, redirect);
        }
    }

    // Quay lại màn chi tiết kèm thông báo lỗi; giữ giờ đã chọn để mở lại pop-up hẹn giờ
    private String backToDetail(Integer articleId, ErrorCode error, String scheduledAt, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", error.getMessage());
        redirect.addFlashAttribute("scheduledAt", scheduledAt);
        return "redirect:/secretary/publications/" + articleId;
    }
}
