package com.group6.electronic_drafting_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.group6.electronic_drafting_system.service.DepartmentHeadService;

import org.springframework.ui.Model;

@Controller
@RequestMapping("/department-head")
public class DepartmentHeadController {
    private final DepartmentHeadService departmentHeadService;

    public DepartmentHeadController(DepartmentHeadService departmentHeadService) {
        this.departmentHeadService = departmentHeadService;
    }

    @GetMapping("/dashboard")
    public String getDepartmentHeadDashboard(Authentication authentication, Model model) {
        String email = currentEmail(authentication);
        model.addAttribute("dashboard", departmentHeadService.getDashboard(email));
        model.addAttribute("loiChao", "Lạy bố mày, trưởng phòng ơi!");
        model.addAttribute("pageTitle", "Dashboard Trưởng phòng");
        model.addAttribute("contentView", "department_head/dashboard");
        model.addAttribute("contentFragment", "departmentHeadContent");

        return "layout/app"; // Trả về tên của template Thymeleaf
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
}
