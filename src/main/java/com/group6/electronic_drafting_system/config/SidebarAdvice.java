package com.group6.electronic_drafting_system.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.service.SidebarService;

import jakarta.servlet.http.HttpServletRequest;

// Đưa menu sidebar (theo quyền của người đăng nhập) và đường dẫn hiện tại vào model của mọi trang
@ControllerAdvice
public class SidebarAdvice {
    private final SidebarService sidebarService;

    public SidebarAdvice(SidebarService sidebarService) {
        this.sidebarService = sidebarService;
    }

    @ModelAttribute
    public void addSidebar(Authentication authentication, HttpServletRequest request, Model model) {
        if (authentication == null || !authentication.isAuthenticated())
            return;
        
        //Lấy danh sách quyền 
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String text = authority.getAuthority(); // "ROLE_DEPARTMENT_HEAD"
            if (!text.startsWith("ROLE_"))
                continue; // không phải role -> bỏ qua

            String name = text.substring("ROLE_".length()); // "DEPARTMENT_HEAD"

            // Tìm role trùng tên trong enum
            RoleName found = null;
            for (RoleName role : RoleName.values()) {
                if (role.name().equals(name)) {
                    found = role;
                    break;
                }
            }

            if (found != null) {
                model.addAttribute("sidebarGroups", sidebarService.menuFor(found));
                break; 
            }
        }

        String fullPath = request.getRequestURI();
        String prefix = request.getContextPath(); // rỗng
        model.addAttribute("activePath", fullPath.substring(prefix.length()));
    }
}
