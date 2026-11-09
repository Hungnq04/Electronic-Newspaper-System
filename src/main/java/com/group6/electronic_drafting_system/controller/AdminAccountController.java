package com.group6.electronic_drafting_system.controller;

import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.service.AccountApprovalService;
import com.group6.electronic_drafting_system.service.AdminAccountService;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Admin: Account List, Account Detail (đổi role / phòng ban), Lock / Unlock, duyệt tài khoản mới đăng ký.
// SecurityConfig chỉ cho ROLE_ADMIN vào /admin/**; service kiểm tra quyền thêm một lần nữa.
@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountController {
    private static final String LIST = "redirect:/admin/accounts";

    private final AdminAccountService adminAccountService;
    private final AccountApprovalService accountApprovalService;   // của TV1

    public AdminAccountController(AdminAccountService adminAccountService, AccountApprovalService accountApprovalService) {
        this.adminAccountService = adminAccountService;
        this.accountApprovalService = accountApprovalService;
    }

    // Hiển thị trong layout chung (sidebar + nội dung), xem templates/layout/app.html
    private static String page(Model model, String title, String view) {
        model.addAttribute("pageTitle", title + " | CMS Core");
        model.addAttribute("contentView", view);
        model.addAttribute("contentFragment", "content");
        model.addAttribute("pageCss", List.of("/css/secretary.css", "/css/admin.css"));
        return "layout/app";
    }

    // ===== Account List =====
    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String role,
                       @RequestParam(required = false) String q,
                       Principal principal, Model model) {
        AccountStatus statusFilter = toStatus(status);
        RoleName roleFilter = toRole(role);
        model.addAttribute("rows", adminAccountService.getAccounts(principal.getName(), statusFilter, roleFilter, q));
        model.addAttribute("counts", adminAccountService.countByStatus(principal.getName()));
        model.addAttribute("statusFilter", statusFilter == null ? "" : statusFilter.name());
        model.addAttribute("roleFilter", roleFilter == null ? "" : roleFilter.name());
        model.addAttribute("q", q == null ? "" : q.trim());
        return page(model, "Quản lý tài khoản", "admin/account-list");
    }

    // ===== Account Detail =====
    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Principal principal, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("acc", adminAccountService.getDetail(id, principal.getName()));
            model.addAttribute("roles", adminAccountService.getGrantableRoles());
            model.addAttribute("departments", adminAccountService.getDepartments());
            return page(model, "Chi tiết tài khoản", "admin/account-detail");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
            return LIST;
        }
    }

    // Đổi role / phòng ban
    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Integer id, @RequestParam(required = false) String role,
                             @RequestParam(required = false) Integer departmentId,
                             Principal principal, RedirectAttributes redirect) {
        try {
            adminAccountService.changeRoleAndDepartment(id, toRole(role), departmentId, principal.getName());
            redirect.addFlashAttribute("message", "Đã cập nhật vai trò và phòng ban.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        }
        return detailPage(id);
    }

    // ===== Lock / Unlock (gọi từ danh sách hoặc từ trang chi tiết; "back" cho biết quay về đâu) =====
    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Integer id, @RequestParam(required = false) String back,
                       Principal principal, RedirectAttributes redirect) {
        try {
            adminAccountService.lock(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã khóa tài khoản. Người này không đăng nhập được nữa.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        }
        return "detail".equals(back) ? detailPage(id) : LIST;
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Integer id, @RequestParam(required = false) String back,
                         Principal principal, RedirectAttributes redirect) {
        try {
            adminAccountService.unlock(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã mở khóa tài khoản.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        }
        return "detail".equals(back) ? detailPage(id) : LIST;
    }

    // ===== Duyệt tài khoản mới đăng ký (AWAITING_APPROVAL) - dùng service của TV1 =====
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Integer id, @RequestParam(required = false) String role,
                          @RequestParam(required = false) Integer departmentId,
                          Principal principal, RedirectAttributes redirect) {
        try {
            accountApprovalService.approve(id, toRole(role), departmentId, principal.getName());
            redirect.addFlashAttribute("message", "Đã duyệt tài khoản. Hệ thống gửi email báo cho người dùng.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        }
        return detailPage(id);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Integer id, @RequestParam(required = false) String reason,
                         Principal principal, RedirectAttributes redirect) {
        try {
            accountApprovalService.reject(id, reason, principal.getName());
            redirect.addFlashAttribute("message", "Đã từ chối yêu cầu đăng ký.");
        } catch (ApplicationException ex) {
            redirect.addFlashAttribute("error", ex.getErrorCode().getMessage());
        }
        return detailPage(id);
    }

    // ===== Hàm phụ =====

    private static String detailPage(Integer id) {
        return "redirect:/admin/accounts/" + id;
    }

    // Chuỗi trên URL / form -> enum; sai hoặc trống thì trả null (service sẽ báo lỗi nếu bắt buộc)
    private static AccountStatus toStatus(String value) {
        try {
            return value == null || value.isBlank() ? null : AccountStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static RoleName toRole(String value) {
        try {
            return value == null || value.isBlank() ? null : RoleName.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
