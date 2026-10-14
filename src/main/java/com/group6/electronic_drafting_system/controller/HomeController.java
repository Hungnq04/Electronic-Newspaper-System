package com.group6.electronic_drafting_system.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    // Màn chính của từng vai trò. Đăng nhập (form / Google) xong đều về "/" rồi chuyển tiếp từ đây.
    // ADMIN chưa có màn riêng nên vẫn ở trang home.
    private static final Map<String, String> DASHBOARD_BY_ROLE = Map.of(
            "ROLE_REPORTER", "/reporter/topics",
            "ROLE_DEPARTMENT_HEAD", "/department-head/dashboard",
            "ROLE_EDITORIAL_SECRETARY", "/secretary/final-reviews");

    @GetMapping("/")
    public String home(Authentication auth) {
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                String target = DASHBOARD_BY_ROLE.get(authority.getAuthority());
                if (target != null) return "redirect:" + target;
            }
        }
        return "home";
    }
}
