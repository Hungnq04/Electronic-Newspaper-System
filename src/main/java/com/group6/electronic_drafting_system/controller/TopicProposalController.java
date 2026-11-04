package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.dto.Topic.TopicCreateRequestDTO;
import com.group6.electronic_drafting_system.entity.enums.TopicStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.service.NotificationService;
import com.group6.electronic_drafting_system.service.TopicService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// FL-01: Topic Proposal List, Create Topic Proposal, Submit Topic Confirmation (modal trên form).
// FL-01 Iter2: Topic Feedback Viewer, Edit & Resubmit Topic, mở thông báo.
// /reporter/** chỉ cho REPORTER (SecurityConfig); service kiểm tra lại quyền.
@Controller
@RequestMapping("/reporter")
public class TopicProposalController {
    private static final String LIST_PATH = "/reporter/topics";
    private final TopicService topicService;
    private final NotificationService notificationService;

    public TopicProposalController(TopicService topicService, NotificationService notificationService) {
        this.topicService = topicService;
        this.notificationService = notificationService;
    }

    // Topic Proposal List
    @GetMapping("/topics")
    public String list(@RequestParam(required = false) String status, Authentication auth, Model model) {
        model.addAttribute("list", topicService.getProposalList(currentEmail(auth), parseStatus(status)));
        return page(model, "Danh sách đề tài – Phóng viên", "reporter/topic-proposal-list", "topicProposalListContent");
    }

    // Create Topic Proposal
    @GetMapping("/topics/new")
    public String createForm(Authentication auth, Model model) {
        if (!model.containsAttribute("topicForm")) model.addAttribute("topicForm", new TopicCreateRequestDTO());
        return formPage(auth, model, null);
    }

    // Submit Topic Confirmation đã được xác nhận trên modal -> lưu Topic (ban = ban của phóng viên)
    @PostMapping("/topics")
    public String create(@Valid @ModelAttribute("topicForm") TopicCreateRequestDTO topicForm, BindingResult binding,
                         Authentication auth, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) return formPage(auth, model, null);
        try {
            topicService.createProposal(currentEmail(auth), topicForm);
        } catch (ApplicationException ex) {
            model.addAttribute("error", ex.getErrorCode().getMessage());
            return formPage(auth, model, null);
        }
        redirect.addFlashAttribute("success", "Đã gửi đề tài thành công. Đề tài đang chờ Trưởng ban duyệt.");
        return "redirect:" + LIST_PATH;
    }

    // Topic Feedback Viewer: lịch sử góp ý / kết quả duyệt (chỉ chủ đề tài)
    @GetMapping("/topics/{topicId}/feedback")
    public String feedback(@PathVariable Integer topicId, Authentication auth, Model model) {
        model.addAttribute("list", topicService.getProposalList(currentEmail(auth), null));
        model.addAttribute("view", topicService.getFeedbackView(currentEmail(auth), topicId));
        return page(model, "Góp ý đề tài – Phóng viên", "reporter/topic-feedback", "topicFeedbackContent");
    }

    // Edit & Resubmit Topic: dùng lại form Create ở chế độ sửa
    @GetMapping("/topics/{topicId}/edit")
    public String editForm(@PathVariable Integer topicId, Authentication auth, Model model) {
        // Kiểm tra quyền trước (ném 403 nếu không phải chủ đề tài hoặc không ở trạng thái cần sửa)
        TopicCreateRequestDTO form = topicService.getEditForm(currentEmail(auth), topicId);
        if (!model.containsAttribute("topicForm")) model.addAttribute("topicForm", form);
        return formPage(auth, model, topicId);
    }

    @PostMapping("/topics/{topicId}/resubmit")
    public String resubmit(@PathVariable Integer topicId, @Valid @ModelAttribute("topicForm") TopicCreateRequestDTO topicForm,
                           BindingResult binding, Authentication auth, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            topicService.getEditForm(currentEmail(auth), topicId); // vẫn kiểm tra quyền trước khi hiện lại form
            return formPage(auth, model, topicId);
        }
        try {
            topicService.resubmit(currentEmail(auth), topicId, topicForm);
        } catch (ApplicationException ex) {
            if (ex.getErrorCode().getStatusCode() == HttpStatus.FORBIDDEN || ex.getErrorCode().getStatusCode() == HttpStatus.NOT_FOUND) throw ex;
            model.addAttribute("error", ex.getErrorCode().getMessage());
            return formPage(auth, model, topicId);
        }
        redirect.addFlashAttribute("success", "Đã gửi lại đề tài. Đề tài quay về hàng chờ Trưởng ban duyệt.");
        return "redirect:" + LIST_PATH;
    }

    // Bấm một thông báo: đánh dấu đã đọc rồi mở trang góp ý của đề tài
    @GetMapping("/notifications/{notificationId}/open")
    public String openNotification(@PathVariable Integer notificationId, Authentication auth) {
        Integer topicId = notificationService.open(currentEmail(auth), notificationId);
        return "redirect:" + LIST_PATH + "/" + topicId + "/feedback";
    }

    // editTopicId == null: tạo mới; khác null: sửa và gửi lại đề tài này
    private String formPage(Authentication auth, Model model, Integer editTopicId) {
        model.addAttribute("list", topicService.getProposalList(currentEmail(auth), null));
        model.addAttribute("myDepartment", topicService.getMyDepartment(currentEmail(auth)));
        model.addAttribute("editTopicId", editTopicId);
        // Chế độ sửa: hiện góp ý yêu cầu sửa gần nhất phía trên form
        if (editTopicId != null) {
            model.addAttribute("latestFeedback", topicService.getFeedbackView(currentEmail(auth), editTopicId).latestChangeRequest());
        }
        model.addAttribute("titleMax", TopicCreateRequestDTO.TITLE_MAX);
        model.addAttribute("summaryMax", TopicCreateRequestDTO.SUMMARY_MAX);
        String title = editTopicId == null ? "Đề xuất đề tài mới – Phóng viên" : "Sửa đề tài – Phóng viên";
        return page(model, title, "reporter/topic-proposal-create", "topicProposalCreateContent");
    }

    private static String page(Model model, String title, String view, String fragment) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("contentView", view);
        model.addAttribute("contentFragment", fragment);
        model.addAttribute("pageCss", List.of("/reporter/css/topic-proposal.css"));
        model.addAttribute("pageJs", List.of("/reporter/js/topic-proposal.js"));
        // Các trang con vẫn sáng mục "Danh sách đề tài" trên sidebar
        model.addAttribute("activePath", LIST_PATH);
        return "layout/app";
    }

    // Sai quyền (403) / không tìm thấy (404): trả đúng mã lỗi HTTP
    @ExceptionHandler(ApplicationException.class)
    public void handleError(ApplicationException ex, HttpServletResponse response) throws IOException {
        ErrorCode code = ex.getErrorCode();
        response.sendError(code.getStatusCode().value(), code.getMessage());
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
