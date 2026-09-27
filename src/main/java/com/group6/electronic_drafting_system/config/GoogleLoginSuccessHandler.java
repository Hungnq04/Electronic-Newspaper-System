package com.group6.electronic_drafting_system.config;

import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Role;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.RoleRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public GoogleLoginSuccessHandler(AccountRepository accounts, RoleRepository roles, PasswordEncoder encoder) {
        this.accounts = accounts; this.roles = roles; this.encoder = encoder;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User google = (OAuth2User) authentication.getPrincipal();
        Object emailAttribute = google.getAttribute("email");
        if (!(emailAttribute instanceof String emailValue) || emailValue.isBlank()) {
            response.sendRedirect("/login?error");
            return;
        }
        String email = emailValue.trim().toLowerCase(Locale.ROOT);
        if (!Boolean.TRUE.equals(google.getAttribute("email_verified"))) {
            response.sendRedirect("/login?error");
            return;
        }
        if (accounts.findByEmailIgnoreCase(email).isEmpty()) {
            Role role = roles.findByRoleNameIgnoreCase("Reporter").orElseGet(() -> {
                Role created = new Role(); created.setRoleName("Reporter"); created.setDescription("Reporter account");
                return roles.save(created);
            });
            Account account = new Account();
            String username = email.substring(0, email.indexOf('@'));
            if (username.length() > 50) username = username.substring(0, 50);
            if (accounts.existsByUsernameIgnoreCase(username)) username = UUID.randomUUID().toString().replace("-", "").substring(0, 50);
            account.setUsername(username);
            account.setEmail(email);
            Object nameAttribute = google.getAttribute("name");
            account.setFullName(nameAttribute instanceof String name && !name.isBlank() ? name : email);
            account.setPasswordHash(encoder.encode("google"));
            account.setRole(role);
            account.setStatus("Active");
            accounts.save(account);
        }
        response.sendRedirect("/");
    }
}
