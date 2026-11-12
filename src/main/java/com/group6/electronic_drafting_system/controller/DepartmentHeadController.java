package com.group6.electronic_drafting_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.service.DepartmentHeadReviewService;
import com.group6.electronic_drafting_system.service.DepartmentHeadService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;

@Controller
@RequestMapping("/department-head")
public class DepartmentHeadController {
    private final DepartmentHeadService departmentHeadService;
    private final DepartmentHeadReviewService departmentHeadReviewService;

    public DepartmentHeadController(DepartmentHeadService departmentHeadService,
            DepartmentHeadReviewService departmentHeadReviewService) {
        this.departmentHeadService = departmentHeadService;
        this.departmentHeadReviewService = departmentHeadReviewService;
    }

    @GetMapping("/dashboard")
    public String getDepartmentHeadDashboard(Authentication authentication, Model model) {
        String email = currentEmail(authentication);
        model.addAttribute("dashboard", departmentHeadService.getDashboard(email));
        model.addAttribute("loiChao", "Lạy bố mày, trưởng phòng ơi!");
        model.addAttribute("pageTitle", "Dashboard Trưởng phòng");
        model.addAttribute("contentView", "department_head/dashboard");
        model.addAttribute("contentFragment", "departmentHeadContent");
        model.addAttribute("css", List.of("/reporter/css/reporter-tasks.css",
                                  "/department_head/css/department-head-dashboard.css"));
        return "layout/app"; // Trả về tên của template Thymeleaf
    }

    @GetMapping("/articles/{articleId}/review")
    public String reviewWorkspace(@PathVariable Integer articleId, Model model, Authentication authentication) { // dẫn
        String email = currentEmail(authentication);
        model.addAttribute("version", departmentHeadReviewService.openArticle(email, articleId));
        model.addAttribute("articleId", articleId);
        model.addAttribute("pageTitle", "Thẩm định bản thảo");
        model.addAttribute("contentView", "department_head/workspacereview");
        model.addAttribute("contentFragment", "reviewWorkspace");
        model.addAttribute("comments", departmentHeadReviewService.getComments(email, articleId));
        model.addAttribute("pageJs", List.of("/department_head/js/workspacereview.js"));
        return "layout/app";
    }

    @PostMapping("/articles/{articleId}/approve")
    public String approve(@PathVariable Integer articleId, @RequestParam(required = false) String feedback,
            Authentication authentication) {
        String email = currentEmail(authentication);
        departmentHeadReviewService.approve(email, articleId, feedback);
        return "redirect:/department-head/dashboard";
    }

    @PostMapping("/articles/{articleId}/request-revision")
    public String requestRevision(@PathVariable Integer articleId, @RequestParam(required = false) String feedback,
            Authentication authentication) {
        String email = currentEmail(authentication);
        departmentHeadReviewService.requestRevision(email, articleId, feedback);
        return "redirect:/department-head/dashboard";
    }

    @PostMapping("/articles/{articleId}/comments")
    public String addComment(@PathVariable Integer articleId,
                         @RequestParam(required = false) String content,
                         @RequestParam(required = false) String anchorField,
                         @RequestParam(required = false) Integer anchorParagraph,
                         @RequestParam(required = false) Integer anchorStart,
                         @RequestParam(required = false) Integer anchorEnd,
                         Authentication authentication) {
    departmentHeadReviewService.addComment(currentEmail(authentication), articleId, content,
            anchorField, anchorParagraph, anchorStart, anchorEnd);
        return "redirect:/department-head/articles/" + articleId + "/review";
    }

    private static String currentEmail(Authentication authentication) {
        // Nếu người đăng nhập là người dùng Google, và có thuộc tính "email" → lấy
        // email đó
        if (authentication.getPrincipal() instanceof OAuth2User google
                && google.getAttribute("email") instanceof String email) {
            return email.trim();
        }
        // Còn lại (đăng nhập mật khẩu): getName() đã là email
        return authentication.getName();
    }

    // Không có quyền, không thấy bài: trả đúng mã HTTP (403, 404...) thay vì lỗi
    // 500
    @ExceptionHandler(ApplicationException.class)
    public void handleError(ApplicationException ex, HttpServletResponse response) throws IOException {
        response.sendError(ex.getErrorCode().getStatusCode().value(), ex.getMessage());
    }
}
