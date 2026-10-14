package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.dto.Topic.TopicCreateRequestDTO;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.service.TopicService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// FL-01: Topic Proposal List, Create Topic Proposal, Submit Topic Confirmation (modal trên form).
// /reporter/** chỉ cho REPORTER (SecurityConfig); service kiểm tra lại quyền khi tạo.
@Controller
@RequestMapping("/reporter/topics")
public class TopicProposalController {
    private static final String LIST_PATH = "/reporter/topics";
    private final TopicService topicService;

    public TopicProposalController(TopicService topicService) {
        this.topicService = topicService;
    }

    // Topic Proposal List
    @GetMapping
    public String list(@RequestParam(required = false) String status, Authentication auth, Model model) {
        model.addAttribute("list", topicService.getProposalList(currentEmail(auth), parseStatus(status)));
        return page(model, "Danh sách đề tài – Phóng viên", "reporter/topic-proposal-list", "topicProposalListContent");
    }

    // Create Topic Proposal
    @GetMapping("/new")
    public String createForm(Authentication auth, Model model) {
        if (!model.containsAttribute("topicForm")) model.addAttribute("topicForm", new TopicCreateRequestDTO());
        return formPage(auth, model);
    }

    // Submit Topic Confirmation đã được xác nhận trên modal -> lưu Topic
    @PostMapping
    public String create(@Valid @ModelAttribute("topicForm") TopicCreateRequestDTO topicForm, BindingResult binding,
                         Authentication auth, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return formPage(auth, model);
        try {
            topicService.createProposal(currentEmail(auth), topicForm);
        } catch (ApplicationException ex) {
            if (ex.getErrorCode() == ErrorCode.TOPIC_DEPARTMENT_INVALID) {
                binding.rejectValue("departmentId", ex.getErrorCode().name(), ex.getErrorCode().getMessage());
            } else {
                model.addAttribute("error", ex.getErrorCode().getMessage());
            }
            return formPage(auth, model);
        }
        redirect.addFlashAttribute("success", "Đã gửi đề tài thành công. Đề tài đang chờ Trưởng ban duyệt.");
        return "redirect:" + LIST_PATH;
    }

    private String formPage(Authentication auth, Model model) {
        model.addAttribute("list", topicService.getProposalList(currentEmail(auth), null));
        model.addAttribute("departments", topicService.getDepartmentOptions());
        model.addAttribute("titleMax", TopicCreateRequestDTO.TITLE_MAX);
        model.addAttribute("summaryMax", TopicCreateRequestDTO.SUMMARY_MAX);
        return page(model, "Đề xuất đề tài mới – Phóng viên", "reporter/topic-proposal-create", "topicProposalCreateContent");
    }

    private static String page(Model model, String title, String view, String fragment) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("contentView", view);
        model.addAttribute("contentFragment", fragment);
        model.addAttribute("pageCss", List.of("/reporter/css/topic-proposal.css"));
        model.addAttribute("pageJs", List.of("/reporter/js/topic-proposal.js"));
        // Trang tạo mới vẫn sáng mục "Danh sách đề tài" trên sidebar
        model.addAttribute("activePath", LIST_PATH);
        return "layout/app";
    }

    // Giá trị lạ trên URL thì coi như "Tất cả"
    private static TopicStatus parseStatus(String status) {
        if (status == null) return null;
        return Arrays.stream(TopicStatus.values()).filter(s -> s.name().equalsIgnoreCase(status.trim())).findFirst().orElse(null);
    }

    // Đăng nhập form: username là email; đăng nhập Google: lấy thuộc tính email
    private static String currentEmail(Authentication auth) {
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof OAuth2User google && google.getAttribute("email") instanceof String email) return email;
        return auth.getName();
    }
}
