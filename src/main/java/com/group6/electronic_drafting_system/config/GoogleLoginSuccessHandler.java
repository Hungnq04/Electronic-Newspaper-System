package com.group6.electronic_drafting_system.config;

import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AccountRepository accounts;
    private final AuthService authService;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

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
            rejectLogin(request, response, ErrorCode.AUTH_ACCOUNT_NOT_REGISTERED);
            return;
        }
        String email = emailValue.trim().toLowerCase(Locale.ROOT);
        var account = accounts.findByEmailIgnoreCase(email);
        if (account.isEmpty()) {
            // Đăng ký bằng Google: email chưa có thì tạo tài khoản chờ Admin duyệt (Google đã xác minh email, không cần OTP)
            try {
                authService.registerWithGoogle(email, google.getAttribute("name") instanceof String name ? name : null);
            } catch (ApplicationException ex) {
                rejectLogin(request, response, ex.getErrorCode());
                return;
            }
            endSession(request);
            response.sendRedirect("/login?registered");
            return;
        }
        AccountStatus status = account.get().getStatus();
        if (status == AccountStatus.AWAITING_APPROVAL) {
            endSession(request);
            response.sendRedirect("/login?awaiting");
            return;
        }
        if (status == AccountStatus.LOCKED || status == AccountStatus.INACTIVE) {
            rejectLogin(request, response, status == AccountStatus.LOCKED
                    ? ErrorCode.AUTH_ACCOUNT_LOCKED : ErrorCode.AUTH_ACCOUNT_INACTIVE);
            return;
        }
        if (status == AccountStatus.PENDING) {
            authService.sendVerificationCodeForExistingAccount(email);
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null) request.getSession(false).invalidate();
            response.sendRedirect("/register/verify?email="
                    + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8) + "&pending=true");
            return;
        }
        ensureRoleAuthority(request, response, authentication, account.get());
        // /dashboard chuyển tiếp tới màn chính của từng vai trò (DashboardController)
        response.sendRedirect("/dashboard");
    }

    // Quyền ROLE_<role> được gắn lúc Google trả về (SecurityConfig.googleRoleMapper). Tài khoản vừa tạo ở trên
    // chưa có lúc đó, nên gắn lại quyền vào phiên đăng nhập để hasRole() / sidebar hoạt động ngay.
    private void ensureRoleAuthority(HttpServletRequest request, HttpServletResponse response,
                                     Authentication authentication, Account account) {
        String role = "ROLE_" + account.getRole().getRoleName().name();
        if (!(authentication instanceof OAuth2AuthenticationToken token)
                || token.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()))) return;
        Set<GrantedAuthority> authorities = new HashSet<>(token.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(role));
        OAuth2AuthenticationToken withRole = new OAuth2AuthenticationToken(
                token.getPrincipal(), authorities, token.getAuthorizedClientRegistrationId());
        withRole.setDetails(token.getDetails());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(withRole);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    private void rejectLogin(HttpServletRequest request, HttpServletResponse response, ErrorCode error) throws IOException {
        endSession(request);
        response.sendRedirect("/login?error=" + error.name());
    }

    // Không giữ phiên đăng nhập Google cho tài khoản chưa được phép vào hệ thống
    private static void endSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) request.getSession(false).invalidate();
    }
}
