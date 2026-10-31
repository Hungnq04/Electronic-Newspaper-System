package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.dto.DepartmentHead.CommentRequestDTO;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.service.DepartmentHeadReviewService;
import com.group6.electronic_drafting_system.service.DepartmentHeadService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/department-head")
public class DepartmentHeadController {
    private static final String NEWSREADER_FONT = "https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,400;0,6..72,500;0,6..72,600;1,6..72,400;1,6..72,500&display=swap";
    private final DepartmentHeadService departmentHeadService;
    private final DepartmentHeadReviewService reviewService;

    public DepartmentHeadController(DepartmentHeadService departmentHeadService, DepartmentHeadReviewService reviewService) {
        this.departmentHeadService = departmentHeadService;
        this.reviewService = reviewService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("dashboard", departmentHeadService.getDashboard(currentEmail(authentication)));
        model.addAttribute("pageTitle", "Duyệt bản thảo – Trưởng ban");
        model.addAttribute("contentView", "department_head/dashboard");
        model.addAttribute("contentFragment", "departmentHeadContent");
        // Dùng chung giao diện của màn /reporter/tasks; file dashboard.css chỉ giữ vài chi tiết riêng
        model.addAttribute("pageCss", List.of("/reporter/css/reporter-tasks.css", "/department_head/css/department-head-dashboard.css"));
        model.addAttribute("pageJs", List.of("/department_head/js/department-head-dashboard.js"));
        return "layout/app";
    }

    // ---------- Màn thẩm định bài viết (workspace) ----------

    // Màn thẩm định nằm trong layout chung (sidebar + nội dung), giống màn dashboard
    @GetMapping("/articles/{articleId}/review")
    public String reviewWorkspace(@PathVariable Integer articleId, Authentication authentication, Model model) {
        model.addAttribute("workspace", reviewService.openWorkspace(currentEmail(authentication), articleId));
        model.addAttribute("pageTitle", "Thẩm định bản thảo – Trưởng ban");
        model.addAttribute("contentView", "department_head/workspacereview");
        model.addAttribute("contentFragment", "reviewWorkspaceContent");
        // Phông Newsreader có chữ nghiêng (cho sapo) + giao diện chung của /reporter/tasks + CSS riêng của màn thẩm định
        model.addAttribute("pageCss", List.of(NEWSREADER_FONT, "/reporter/css/reporter-tasks.css", "/department_head/css/department-head-workspace-review.css"));
        model.addAttribute("pageJs", List.of("/department_head/js/department-head-workspace-review.js"));
        // Đang thẩm định một bài thì sidebar vẫn sáng mục "Duyệt bản thảo"
        model.addAttribute("activePath", "/department-head/dashboard");
        return "layout/app";
    }

    @PostMapping("/articles/{articleId}/comments")
    public String addComment(@PathVariable Integer articleId, @ModelAttribute CommentRequestDTO request,
                             Authentication authentication, RedirectAttributes flash) {
        return runAndReturn(articleId, flash, "Đã lưu góp ý.",
                () -> reviewService.addComment(currentEmail(authentication), articleId, request));
    }

    @PostMapping("/articles/{articleId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Integer articleId, @PathVariable Integer commentId,
                                Authentication authentication, RedirectAttributes flash) {
        return runAndReturn(articleId, flash, "Đã xóa góp ý.",
                () -> reviewService.deleteComment(currentEmail(authentication), articleId, commentId));
    }

    @PostMapping("/articles/{articleId}/comments/{commentId}/resolve")
    public String resolveComment(@PathVariable Integer articleId, @PathVariable Integer commentId,
                                 @RequestParam boolean resolved, Authentication authentication, RedirectAttributes flash) {
        return runAndReturn(articleId, flash, resolved ? "Đã đánh dấu: Lead đã sửa." : "Đã đánh dấu: Lead chưa sửa.",
                () -> reviewService.setPreviousCommentResolved(currentEmail(authentication), articleId, commentId, resolved));
    }

    @PostMapping("/articles/{articleId}/approve")
    public String approve(@PathVariable Integer articleId, @RequestParam(required = false) String feedback,
                          Authentication authentication, RedirectAttributes flash) {
        return runAndReturn(articleId, flash, "Đã duyệt cấp phòng. Bài chuyển sang Thư ký tòa soạn.",
                () -> reviewService.approve(currentEmail(authentication), articleId, feedback));
    }

    @PostMapping("/articles/{articleId}/request-revision")
    public String requestRevision(@PathVariable Integer articleId, @RequestParam(required = false) String feedback,
                                  Authentication authentication, RedirectAttributes flash) {
        return runAndReturn(articleId, flash, "Đã trả bài cho Lead chỉnh sửa.",
                () -> reviewService.requestRevision(currentEmail(authentication), articleId, feedback));
    }

    // Lỗi khi MỞ trang (không có quyền, không tìm thấy bài...): trả đúng mã HTTP của ErrorCode (403, 404...)
    @ExceptionHandler(ApplicationException.class)
    public void handleError(ApplicationException ex, HttpServletResponse response) throws IOException {
        response.sendError(ex.getErrorCode().getStatusCode().value(), ex.getMessage());
    }

    // Chạy một thao tác rồi quay lại trang workspace, kèm thông báo thành công hoặc lỗi (hiện dạng toast)
    private static String runAndReturn(Integer articleId, RedirectAttributes flash, String successMessage, Runnable action) {
        try {
            action.run();
            flash.addFlashAttribute("toast", successMessage);
        } catch (ApplicationException ex) {
            flash.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/department-head/articles/" + articleId + "/review";
    }

    // Đăng nhập mật khẩu: getName() là email. Đăng nhập Google: getName() là mã "sub" của Google, email nằm trong thuộc tính "email"
    private static String currentEmail(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User google && google.getAttribute("email") instanceof String email) {
            return email.trim();
        }
        return authentication.getName();
    }
}
