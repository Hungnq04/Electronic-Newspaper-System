package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.dto.DepartmentHead.ChangeLeadFormDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentFormDTO;
import com.group6.electronic_drafting_system.dto.DepartmentHead.TopicAssignmentPageDTO;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.DepartmentHeadTopicMapper;
import com.group6.electronic_drafting_system.service.DepartmentHeadTopicService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/department-head")
public class DepartmentHeadController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DepartmentHeadController.class);
    private static final String TOPICS_PATH = "/department-head/topics";
    private final DepartmentHeadTopicService departmentHeadTopicService;

    public DepartmentHeadController(DepartmentHeadTopicService departmentHeadTopicService) {
        this.departmentHeadTopicService = departmentHeadTopicService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Duyệt bản thảo – Trưởng ban");
        model.addAttribute("contentView", "department_head/dashboard");
        model.addAttribute("contentFragment", "departmentHeadContent");
        model.addAttribute("pageCss", List.of("/department_head/css/department-head-dashboard.css"));
        model.addAttribute("pageJs", List.of("/department_head/js/department-head-dashboard.js"));
        return "layout/app";
    }

    // Danh sách đề tài ACTIVE mà Trưởng phòng đang phụ trách, kèm tình trạng giao việc
    @GetMapping("/topics")
    public String topics(Authentication auth, Model model) {
        model.addAttribute("board", departmentHeadTopicService.getBoard(currentEmail(auth)));
        model.addAttribute("pageTitle", "Đề tài đã duyệt – Trưởng ban");
        model.addAttribute("contentView", "department_head/topics");
        model.addAttribute("contentFragment", "departmentHeadTopicsContent");
        model.addAttribute("pageCss", List.of("/department_head/css/department-head-topics.css"));
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
            return "redirect:" + TOPICS_PATH;
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
            return "redirect:" + TOPICS_PATH;
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
            return "redirect:" + TOPICS_PATH;
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
        model.addAttribute("pageCss", List.of("/department_head/css/department-head-topics.css", "/department_head/css/department-head-assignment.css"));
        model.addAttribute("pageJs", List.of("/department_head/js/department-head-assignment.js"));
        // Trang giao việc vẫn sáng mục "Đề tài đã duyệt" trên sidebar
        model.addAttribute("activePath", TOPICS_PATH);
        return "layout/app";
    }

    // Đăng nhập form: username là email; đăng nhập Google: lấy thuộc tính email
    private static String currentEmail(Authentication auth) {
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof OAuth2User google && google.getAttribute("email") instanceof String email) return email;
        return auth.getName();
    }
}
