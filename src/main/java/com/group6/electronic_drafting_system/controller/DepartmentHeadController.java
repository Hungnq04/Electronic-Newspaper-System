package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ChangeLeadFormDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.CommentRequestDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentFormDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentPageDTO;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.DepartmentHeadTopicMapper;
import com.group6.electronic_drafting_system.service.DepartmentHeadReviewService;
import com.group6.electronic_drafting_system.service.DepartmentHeadService;
import com.group6.electronic_drafting_system.service.DepartmentHeadTopicService;
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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DepartmentHeadController.class);
    private static final String TOPICS_PATH = "/department-head/topics";
    // Trang danh sách đề tài cần giao việc. URL /department-head/topics thuộc màn Duyệt đề tài (DepartmentHeadTopicController).
    private static final String APPROVED_TOPICS_PATH = "/department-head/approved-topics";
    private static final String NEWSREADER_FONT = "https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,400;0,6..72,500;0,6..72,600;1,6..72,400;1,6..72,500&display=swap";
    private final DepartmentHeadService departmentHeadService;
    private final DepartmentHeadReviewService reviewService;
    private final DepartmentHeadTopicService departmentHeadTopicService;

    public DepartmentHeadController(DepartmentHeadService departmentHeadService, DepartmentHeadReviewService reviewService,
                                    DepartmentHeadTopicService departmentHeadTopicService) {
        this.departmentHeadService = departmentHeadService;
        this.reviewService = reviewService;
        this.departmentHeadTopicService = departmentHeadTopicService;
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

    // Danh sách đề tài ACTIVE mà Trưởng phòng đang phụ trách, kèm tình trạng giao việc
    @GetMapping("/approved-topics")
    public String topics(Authentication auth, Model model) {
        model.addAttribute("board", departmentHeadTopicService.getBoard(currentEmail(auth)));
        model.addAttribute("pageTitle", "Đề tài đã duyệt – Trưởng ban");
        model.addAttribute("contentView", "department_head/topics");
        model.addAttribute("contentFragment", "departmentHeadTopicsContent");
        // Dùng chung giao diện rp- (reporter-tasks.css) như Dashboard; department-head-topics.css chỉ giữ vài chi tiết riêng
        model.addAttribute("pageCss", List.of("/reporter/css/reporter-tasks.css", "/department_head/css/department-head-topics.css"));
        model.addAttribute("pageJs", List.of("/department_head/js/department-head-topics.js"));
        return "layout/app";
    }

    // Trang giao việc một đề tài (FL-03). Không phụ trách đề tài này thì quay về danh sách kèm thông báo lỗi.
    @GetMapping("/topics/{topicId}/assignment")
    public String assignment(@PathVariable Integer topicId, Authentication auth, Model model, RedirectAttributes redirect) {
        TopicAssignmentPageDTO page;
        try {
            page = departmentHeadTopicService.getAssignmentPage(topicId, currentEmail(auth));
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return "redirect:" + APPROVED_TOPICS_PATH;
        }
        TopicAssignmentFormDTO form = TopicAssignmentFormDTO.prefill(
                DepartmentHeadTopicMapper.toInputValue(page.leadDeadline()), page.leadRequirements());
        return assignmentPage(model, page, form, new ChangeLeadFormDTO());
    }

    // Xác nhận giao việc. Lỗi kiểm tra thì hiện lại form với dữ liệu đã nhập để người dùng sửa.
    @PostMapping("/topics/{topicId}/assignment")
    public String confirmAssignment(@PathVariable Integer topicId, @ModelAttribute TopicAssignmentFormDTO form,
                                    Authentication auth, Model model, RedirectAttributes redirect) {
        String email = currentEmail(auth);
        try {
            departmentHeadTopicService.confirmAssignment(topicId, form, email);
            redirect.addFlashAttribute("message", "Đã giao việc cho đề tài #" + topicId + ".");
            return "redirect:" + APPROVED_TOPICS_PATH;
        } catch (ApplicationException ex) {
            model.addAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Confirm assignment failed for topic {}", topicId, ex);
            model.addAttribute("error", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }
        return showAgain(topicId, email, model, redirect, form, new ChangeLeadFormDTO());
    }

    // ===== Quản lý nhóm (đề tài đã giao xong) =====

    // Thêm Supporting vào nhóm
    @PostMapping("/topics/{topicId}/supporters")
    public String addSupporters(@PathVariable Integer topicId, @ModelAttribute TopicAssignmentFormDTO form,
                                Authentication auth, Model model, RedirectAttributes redirect) {
        String email = currentEmail(auth);
        try {
            departmentHeadTopicService.addSupporters(topicId, form, email);
            redirect.addFlashAttribute("message", "Đã thêm phóng viên hỗ trợ vào nhóm.");
            return "redirect:" + assignmentPath(topicId);
        } catch (ApplicationException ex) {
            model.addAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Add supporters failed for topic {}", topicId, ex);
            model.addAttribute("error", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }
        return showAgain(topicId, email, model, redirect, form, new ChangeLeadFormDTO());
    }

    // Gỡ một Supporting khỏi nhóm (assignment -> REMOVED)
    @PostMapping("/topics/{topicId}/supporters/{assignmentId}/remove")
    public String removeSupporter(@PathVariable Integer topicId, @PathVariable Integer assignmentId,
                                  Authentication auth, RedirectAttributes redirect) {
        try {
            departmentHeadTopicService.removeSupporter(topicId, assignmentId, currentEmail(auth));
            redirect.addFlashAttribute("message", "Đã gỡ phóng viên khỏi nhóm. Tư liệu họ đã thêm vẫn được giữ.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Remove supporter {} failed for topic {}", assignmentId, topicId, ex);
            redirect.addFlashAttribute("error", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }
        return "redirect:" + assignmentPath(topicId);
    }

    // Đổi Lead
    @PostMapping("/topics/{topicId}/lead")
    public String changeLead(@PathVariable Integer topicId, @ModelAttribute("leadForm") ChangeLeadFormDTO leadForm,
                             Authentication auth, Model model, RedirectAttributes redirect) {
        String email = currentEmail(auth);
        try {
            departmentHeadTopicService.changeLead(topicId, leadForm, email);
            redirect.addFlashAttribute("message", "Đã đổi Lead. Bài nháp / bài cần sửa của Lead cũ đã chuyển sang Lead mới.");
            return "redirect:" + assignmentPath(topicId);
        } catch (ApplicationException ex) {
            model.addAttribute("error", ex.getErrorCode().getMessage());
        } catch (Exception ex) {
            log.error("Change lead failed for topic {}", topicId, ex);
            model.addAttribute("error", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        }
        model.addAttribute("leadFormOpen", true);   // mở sẵn khung "Đổi Lead" để người dùng sửa lỗi
        return showAgain(topicId, email, model, redirect, new TopicAssignmentFormDTO(), leadForm);
    }

    // Hiện lại trang (kèm thông báo lỗi đã đặt trong model) với dữ liệu người dùng vừa nhập
    private String showAgain(Integer topicId, String email, Model model, RedirectAttributes redirect,
                             TopicAssignmentFormDTO form, ChangeLeadFormDTO leadForm) {
        try {
            return assignmentPage(model, departmentHeadTopicService.getAssignmentPage(topicId, email), form, leadForm);
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return "redirect:" + APPROVED_TOPICS_PATH;
        }
    }

    private static String assignmentPath(Integer topicId) {
        return TOPICS_PATH + "/" + topicId + "/assignment";
    }

    private static String assignmentPage(Model model, TopicAssignmentPageDTO page, TopicAssignmentFormDTO form, ChangeLeadFormDTO leadForm) {
        model.addAttribute("page", page);
        model.addAttribute("form", form);
        model.addAttribute("leadForm", leadForm);
        model.addAttribute("pageTitle", "Giao việc đề tài – Trưởng ban");
        model.addAttribute("contentView", "department_head/topic-assignment");
        model.addAttribute("contentFragment", "departmentHeadAssignmentContent");
        // Thứ tự nạp: giao diện chung rp- -> chi tiết dht- -> chi tiết riêng của trang giao việc (dha-)
        model.addAttribute("pageCss", List.of("/reporter/css/reporter-tasks.css", "/department_head/css/department-head-topics.css", "/department_head/css/department-head-assignment.css"));
        model.addAttribute("pageJs", List.of("/department_head/js/department-head-assignment.js"));
        // Trang giao việc vẫn sáng mục "Đề tài đã duyệt" trên sidebar
        model.addAttribute("activePath", APPROVED_TOPICS_PATH);
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
        if (authentication == null) return null;
        if (authentication.getPrincipal() instanceof OAuth2User google && google.getAttribute("email") instanceof String email) {
            return email.trim();
        }
        return authentication.getName();
    }
}
