package com.group6.electronic_drafting_system.config;

import com.group6.electronic_drafting_system.controller.EditorialTopicController;
import com.group6.electronic_drafting_system.controller.FinalReviewController;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Thanh trên cùng (fragment/secretary-topbar) của các màn Thư ký cần tên + chữ viết tắt của người đăng nhập.
// Chỉ áp dụng cho các controller liệt kê ở assignableTypes, không ảnh hưởng màn của role khác.
@ControllerAdvice(assignableTypes = {FinalReviewController.class, EditorialTopicController.class})
public class SecretaryHeaderAdvice {
    private final AccountRepository accountRepository;

    public SecretaryHeaderAdvice(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @ModelAttribute
    public void addHeader(Authentication authentication, Model model) {
        if (authentication == null) return;
        String fullName = accountRepository.findByEmailIgnoreCase(authentication.getName())
                .map(Account::getFullName)
                .orElse(authentication.getName());
        model.addAttribute("headerName", fullName);
        model.addAttribute("headerRole", "Thư ký tòa soạn");
        model.addAttribute("headerInitials", initials(fullName));
    }

    // "Lê Minh Châu" -> "LC": chữ đầu của họ + chữ đầu của tên
    private String initials(String fullName) {
        String[] words = fullName.trim().split("\\s+");
        String first = words[0].substring(0, 1);
        String last = words.length > 1 ? words[words.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
