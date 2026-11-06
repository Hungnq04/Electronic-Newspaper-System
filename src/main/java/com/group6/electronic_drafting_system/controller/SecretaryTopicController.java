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

// FL-01 Iter2: Final Topic Review Workspace của Thư ký (+ modal Final Topic Approval Confirmation, Reject Topic Form).
// /secretary/** chỉ cho EDITORIAL_SECRETARY (SecurityConfig); service kiểm tra lại role và trạng thái.
@Controller
@RequestMapping("/secretary/topic-reviews")
public class SecretaryTopicController {
    private static final String PATH = "/secretary/topic-reviews";
    private final TopicReviewService topicReviewService;

    public SecretaryTopicController(TopicReviewService topicReviewService) {
        this.topicReviewService = topicReviewService;
    }

    // department: lọc theo ban (không có = mọi ban); topic: đề tài đang mở chi tiết
    @GetMapping
    public String workspace(@RequestParam(required = false) Integer department, @RequestParam(required = false) Integer topic,
                            Principal principal, Model model) {
        model.addAttribute("ws", topicReviewService.getSecretaryWorkspace(principal.getName(), department, topic));
        model.addAttribute("pageTitle", "Duyệt đề tài cuối | CMS Core");
        model.addAttribute("contentView", "secretary/topic-final-review");
        model.addAttribute("contentFragment", "content");
        model.addAttribute("pageCss", List.of("/reporter/css/topic-proposal.css", "/css/topic-review.css"));
        model.addAttribute("activePath", PATH);
        return "layout/app";
    }

    @PostMapping("/{topicId}/approve")
    public String approve(@PathVariable Integer topicId, Principal principal, RedirectAttributes redirect) {
        try {
            topicReviewService.secretaryApprove(principal.getName(), topicId);
            redirect.addFlashAttribute("message", "Đã duyệt đề tài. Đề tài chuyển sang ACTIVE và đã giao Lead / Trưởng ban.");
            return "redirect:" + PATH;
        } catch (ApplicationException ex) {
            return backWithError(ex, topicId, null, redirect);
        }
    }

    @PostMapping("/{topicId}/reject")
    public String reject(@PathVariable Integer topicId, @RequestParam(required = false) String reason,
                         Principal principal, RedirectAttributes redirect) {
        try {
            topicReviewService.secretaryReject(principal.getName(), topicId, reason);
            redirect.addFlashAttribute("message", "Đã từ chối đề tài. Phóng viên sẽ thấy lý do từ chối.");
            return "redirect:" + PATH;
        } catch (ApplicationException ex) {
            return backWithError(ex, topicId, reason, redirect);
        }
    }

    // Lỗi nghiệp vụ (đã được xử lý, thiếu lý do...): quay lại workspace và báo lỗi, giữ nội dung đã nhập.
    // Sai quyền / không tồn tại: ném tiếp để trả 403 / 404.
    private static String backWithError(ApplicationException ex, Integer topicId, String reason, RedirectAttributes redirect) {
        HttpStatus status = ex.getErrorCode().getStatusCode();
        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.NOT_FOUND) throw ex;
        redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        redirect.addFlashAttribute("reason", reason);
        return "redirect:" + PATH + "?topic=" + topicId;
    }

    @ExceptionHandler(ApplicationException.class)
    public void handleError(ApplicationException ex, HttpServletResponse response) throws IOException {
        response.sendError(ex.getErrorCode().getStatusCode().value(), ex.getMessage());
    }
}
