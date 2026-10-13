package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.service.ReporterTaskService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reporter")
public class ReporterController {
    private static final String TASKS_PATH = "/reporter/tasks";
    private final ReporterTaskService reporterTaskService;

    public ReporterController(ReporterTaskService reporterTaskService) {
        this.reporterTaskService = reporterTaskService;
    }

    @GetMapping("/tasks")
    public String tasks(Authentication auth, Model model) {
        model.addAttribute("board", reporterTaskService.getBoard(currentEmail(auth)));
        return page(model, "Nhiệm vụ tác nghiệp – Phóng viên", "reporter/tasks", "reporterTasksContent");
    }

    @GetMapping("/tasks/{assignmentId}")
    public String taskDetail(@PathVariable Integer assignmentId, Authentication auth, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("detail", reporterTaskService.getDetail(assignmentId, currentEmail(auth)));
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return "redirect:" + TASKS_PATH;
        }
        return page(model, "Chi tiết nhiệm vụ – Phóng viên", "reporter/task-detail", "reporterTaskDetailContent");
    }

    private static String page(Model model, String title, String view, String fragment) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("contentView", view);
        model.addAttribute("contentFragment", fragment);
        model.addAttribute("pageCss", List.of("/reporter/css/reporter-tasks.css"));
        model.addAttribute("pageJs", List.of("/reporter/js/reporter-tasks.js"));
        // Trang chi tiết vẫn sáng mục "Nhiệm vụ tác nghiệp" trên sidebar
        model.addAttribute("activePath", TASKS_PATH);
        return "layout/app";
    }

    // Đăng nhập form: username là email; đăng nhập Google: lấy thuộc tính email
    private static String currentEmail(Authentication auth) {
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof OAuth2User google && google.getAttribute("email") instanceof String email) return email;
        return auth.getName();
    }
}
