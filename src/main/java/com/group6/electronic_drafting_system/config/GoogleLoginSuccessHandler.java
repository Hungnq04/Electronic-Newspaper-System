package com.group6.electronic_drafting_system.config;

import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AccountRepository accounts;
    private final AuthService authService;

    public GoogleLoginSuccessHandler(AccountRepository accounts, AuthService authService) {
        this.accounts = accounts;
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User google = (OAuth2User) authentication.getPrincipal();
        Object emailAttribute = google.getAttribute("email");
        if (!(emailAttribute instanceof String emailValue) || emailValue.isBlank()
                || !Boolean.TRUE.equals(google.getAttribute("email_verified"))) {
            rejectLogin(request, response);
            return;
        }
        String email = emailValue.trim().toLowerCase(Locale.ROOT);
        var account = accounts.findByEmailIgnoreCase(email);
        if (account.isEmpty()) {
            rejectLogin(request, response);
            return;
        }
        if (!Boolean.TRUE.equals(account.get().getVerified())) {
            authService.sendVerificationCodeForExistingAccount(email);
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null) request.getSession(false).invalidate();
            response.sendRedirect("/register/verify?email="
                    + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8) + "&pending=true");
            return;
        }
        response.sendRedirect("/");
    }

    private void rejectLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        response.sendRedirect("/login?error=" + ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED.name());
    }
}
