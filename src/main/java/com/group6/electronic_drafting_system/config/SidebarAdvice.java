package com.group6.electronic_drafting_system.config;

import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.service.SidebarService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Đưa menu sidebar (theo quyền của người đăng nhập) và đường dẫn hiện tại vào model của mọi trang
@ControllerAdvice
public class SidebarAdvice {
    private final SidebarService sidebarService;

    public SidebarAdvice(SidebarService sidebarService) {
        this.sidebarService = sidebarService;
    }

    @ModelAttribute
    public void addSidebar(Authentication authentication, HttpServletRequest request, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return;
        authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .filter(name -> Arrays.stream(RoleName.values()).anyMatch(role -> role.name().equals(name)))
                .findFirst()
                .ifPresent(name -> model.addAttribute("sidebarGroups", sidebarService.menuFor(RoleName.valueOf(name))));
        model.addAttribute("activePath", request.getRequestURI().substring(request.getContextPath().length()));
    }
}
