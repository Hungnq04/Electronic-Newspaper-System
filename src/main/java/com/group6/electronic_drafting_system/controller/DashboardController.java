package com.group6.electronic_drafting_system.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Đăng nhập (form / Google) xong đều về /dashboard rồi chuyển tới màn chính của vai trò.
// "/" là trang chủ công khai FL-07 (PublicArticleController) nên không dùng "/" cho việc này.
@Controller
public class DashboardController {
    private static final Map<String, String> DASHBOARD_BY_ROLE = Map.of(
            "ROLE_REPORTER", "/reporter/topics",
            "ROLE_DEPARTMENT_HEAD", "/department-head/dashboard",
            "ROLE_EDITORIAL_SECRETARY", "/secretary/final-reviews");

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                String target = DASHBOARD_BY_ROLE.get(authority.getAuthority());
                if (target != null) return "redirect:" + target;
            }
        }
        // ADMIN chưa có màn riêng: về trang chủ
        return "redirect:/";
    }
}
