package com.group6.electronic_drafting_system.config;

import com.group6.electronic_drafting_system.dto.Header.CurrentUserDTO;
import com.group6.electronic_drafting_system.service.CurrentUserService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Đưa người đang đăng nhập (headerUser) và ngày hôm nay (headerToday) vào model của mọi trang,
// để layout/app.html gắn header chung (fragment/header.html). Cách làm giống SidebarAdvice.
// Không đặt tên "currentUser" vì trang công khai (PublicArticleController) đã dùng tên đó.
@ControllerAdvice
public class CurrentUserAdvice {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    // "EEEE" = tên thứ, Locale "vi" cho ra tiếng Việt, vd "Thứ Sáu, 30/10/2026"
    private static final DateTimeFormatter TODAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi"));
    private final CurrentUserService currentUserService;

    public CurrentUserAdvice(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @ModelAttribute
    public void addCurrentUser(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return;
        CurrentUserDTO user = currentUserService.findByEmail(currentEmail(authentication));
        if (user == null) return;
        model.addAttribute("headerUser", user);
        model.addAttribute("headerToday", LocalDate.now(VIETNAM).format(TODAY_FORMAT));
    }

    // Đăng nhập form: username là email; đăng nhập Google: lấy thuộc tính email
    private static String currentEmail(Authentication auth) {
        if (auth.getPrincipal() instanceof OAuth2User google && google.getAttribute("email") instanceof String email) return email;
        return auth.getName();
    }
}
