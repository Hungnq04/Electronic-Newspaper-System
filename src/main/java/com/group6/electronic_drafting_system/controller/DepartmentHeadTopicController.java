package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.service.TopicReviewService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// FL-01 Iter2: Topic Review Workspace của Trưởng ban (+ modal Approve Topic Confirmation, Request Changes Form).
// /department-head/** chỉ cho DEPARTMENT_HEAD (SecurityConfig); service kiểm tra lại ban và trạng thái.
@Controller
@RequestMapping("/department-head/topics")
public class DepartmentHeadTopicController {
    private static final String PATH = "/department-head/topics";
    private final TopicReviewService topicReviewService;

    public DepartmentHeadTopicController(TopicReviewService topicReviewService) {
        this.topicReviewService = topicReviewService;
    }

    @GetMapping
    public String workspace(@RequestParam(required = false) Integer topic, Principal principal, Model model) {
        model.addAttribute("ws", topicReviewService.getHeadWorkspace(principal.getName(), topic));
        model.addAttribute("pageTitle", "Duyệt đề tài | CMS Core");
        model.addAttribute("contentView", "department_head/topic-review");
        model.addAttribute("contentFragment", "content");
        model.addAttribute("pageCss", List.of("/reporter/css/topic-proposal.css", "/css/topic-review.css"));
        model.addAttribute("activePath", PATH);
        return "layout/app";
    }

    @PostMapping("/{topicId}/approve")
    public String approve(@PathVariable Integer topicId, Principal principal, RedirectAttributes redirect) {
        try {
            topicReviewService.headApprove(principal.getName(), topicId);
            redirect.addFlashAttribute("message", "Đã duyệt đề tài. Đề tài được chuyển lên Thư ký tòa soạn.");
            return "redirect:" + PATH;
        } catch (ApplicationException ex) {
            return backWithError(ex, topicId, null, redirect);
        }
    }

    @PostMapping("/{topicId}/request-changes")
    public String requestChanges(@PathVariable Integer topicId, @RequestParam(required = false) String feedback,
                                 Principal principal, RedirectAttributes redirect) {
        try {
            topicReviewService.headRequestChanges(principal.getName(), topicId, feedback);
            redirect.addFlashAttribute("message", "Đã gửi yêu cầu sửa cho phóng viên.");
            return "redirect:" + PATH;
        } catch (ApplicationException ex) {
            return backWithError(ex, topicId, feedback, redirect);
        }
    }

    // Lỗi nghiệp vụ (đã được xử lý, thiếu góp ý...): quay lại workspace và báo lỗi, giữ nội dung đã nhập.
    // Sai quyền / không tồn tại: ném tiếp để trả 403 / 404.
    private static String backWithError(ApplicationException ex, Integer topicId, String feedback, RedirectAttributes redirect) {
        HttpStatus status = ex.getErrorCode().getStatusCode();
        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.NOT_FOUND) throw ex;
        redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        redirect.addFlashAttribute("feedback", feedback);
        return "redirect:" + PATH + "?topic=" + topicId;
    }

    @ExceptionHandler(ApplicationException.class)
    public void handleError(ApplicationException ex, HttpServletResponse response) throws IOException {
        response.sendError(ex.getErrorCode().getStatusCode().value(), ex.getMessage());
    }
}
