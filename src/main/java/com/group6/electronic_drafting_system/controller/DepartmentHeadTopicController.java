package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.service.TopicReviewService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
// [TV1-1.1] Nói với thầy: "Chỗ này em khai báo controller cho các URL /department-head/topics và nhận
//   TopicReviewService để gọi. SecurityConfig đã chỉ cho Trưởng ban vào các URL này."
@Controller
@RequestMapping("/department-head/topics")
public class DepartmentHeadTopicController {
    private static final String PATH = "/department-head/topics";
    private final TopicReviewService topicReviewService;

    public DepartmentHeadTopicController(TopicReviewService topicReviewService) {
        this.topicReviewService = topicReviewService;
    }
//dautienduyet
    // [TV1-1.2] Nói với thầy: "Chỗ này em mở màn duyệt: lấy id đề tài từ ?topic= trên URL (khi bấm đề tài ở hàng chờ
    //   [TV1-1.4]), gọi service lấy dữ liệu [TV1-1.7], bỏ vào Model tên ws rồi trả về layout để vẽ màn [TV1-1.3]."
    @GetMapping
    public String workspace(@RequestParam(required = false) Integer topic, Principal principal, Model model) {
        model.addAttribute("ws", topicReviewService.getHeadWorkspace(currentEmail(principal), topic));
        model.addAttribute("pageTitle", "Duyệt đề tài | CMS Core");
        model.addAttribute("contentView", "department_head/topic-review");
        model.addAttribute("contentFragment", "content");
        model.addAttribute("pageCss", List.of("/reporter/css/topic-proposal.css", "/css/topic-review.css"));
        model.addAttribute("activePath", PATH);
        return "layout/app";
    }

    // [TV1-1.14] Nói với thầy: "Chỗ này em xử lý nút Duyệt: lấy id đề tài trên đường dẫn, gọi service headApprove
    //   [TV1-1.15]. Xong thì quay lại màn duyệt kèm thông báo xanh; lỗi thì quay lại kèm chữ đỏ [TV1-11.1]."
    @PostMapping("/{topicId}/approve")
    public String approve(@PathVariable Integer topicId, Principal principal, RedirectAttributes redirect) {
        try {
            topicReviewService.headApprove(currentEmail(principal), topicId);
            redirect.addFlashAttribute("message", "Đã duyệt đề tài. Đề tài được chuyển lên Thư ký tòa soạn.");
            return "redirect:" + PATH;
        } catch (ApplicationException ex) {
            return backWithError(ex, topicId, null, redirect);
        }
    }

    // [TV1-1.17] Nói với thầy: "Chỗ này em xử lý nút Yêu cầu sửa: lấy id đề tài và nội dung góp ý, gọi service
    //   headRequestChanges [TV1-1.18]. Lỗi thì quay lại màn, giữ nguyên góp ý đã gõ."
    @PostMapping("/{topicId}/request-changes")
    public String requestChanges(@PathVariable Integer topicId, @RequestParam(required = false) String feedback,
                                 Principal principal, RedirectAttributes redirect) {
        try {
            topicReviewService.headRequestChanges(currentEmail(principal), topicId, feedback);
            redirect.addFlashAttribute("message", "Đã gửi yêu cầu sửa cho phóng viên.");
            return "redirect:" + PATH;
        } catch (ApplicationException ex) {
            return backWithError(ex, topicId, feedback, redirect);
        }
    }

    // Lỗi nghiệp vụ (đã được xử lý, thiếu góp ý...): quay lại workspace và báo lỗi, giữ nội dung đã nhập.
    // Sai quyền / không tồn tại: ném tiếp để trả 403 / 404.
    // [TV1-11.1] Nói với thầy: "Chỗ này em xử lý khi gặp lỗi: lỗi do thao tác (thiếu góp ý, đề tài đã được xử lý) thì
    //   quay lại màn báo chữ đỏ; lỗi sai quyền hoặc không tìm thấy thì ném tiếp để hiện trang 403 / 404 [TV1-11.2]."
    private static String backWithError(ApplicationException ex, Integer topicId, String feedback, RedirectAttributes redirect) {
        HttpStatus status = ex.getErrorCode().getStatusCode();
        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.NOT_FOUND) throw ex;
        redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        redirect.addFlashAttribute("feedback", feedback);
        return "redirect:" + PATH + "?topic=" + topicId;
    }

    // Lấy email người đăng nhập. Đăng nhập bằng mật khẩu: getName() chính là email.
    // Đăng nhập Google: getName() là mã định danh của Google ("sub"), nên phải đọc thuộc tính "email"
    // (cùng cách với TopicProposalController.currentEmail).
    // [TV1-8.3] Nói với thầy: "Chỗ này em lấy email của người đang đăng nhập: đăng nhập mật khẩu thì lấy getName(),
    //   đăng nhập Google thì đọc thuộc tính email. Service dùng email này để tìm tài khoản và kiểm tra quyền."
    private static String currentEmail(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof OAuth2User google
                && google.getAttribute("email") instanceof String email) return email;
        return principal.getName();
    }

    // [TV1-11.2] Nói với thầy: "Chỗ này em bắt lỗi còn sót của controller này và trả đúng mã HTTP ghi trong ErrorCode
    //   (403, 404...) để hiện trang lỗi."
    @ExceptionHandler(ApplicationException.class)
    public void handleError(ApplicationException ex, HttpServletResponse response) throws IOException {
        response.sendError(ex.getErrorCode().getStatusCode().value(), ex.getMessage());
    }
}
