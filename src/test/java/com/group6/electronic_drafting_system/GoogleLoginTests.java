package com.group6.electronic_drafting_system;

import static org.assertj.core.api.Assertions.assertThat;

import com.group6.electronic_drafting_system.config.GoogleLoginSuccessHandler;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;

// Đăng nhập / đăng ký bằng Google: giả lập bước Google trả về rồi gọi thẳng success handler.
// @Transactional: tài khoản tạo trong test được rollback, không để lại dữ liệu.
@SpringBootTest
@Transactional
class GoogleLoginTests {
    @Autowired GoogleLoginSuccessHandler handler;
    @Autowired AccountRepository accounts;

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void newGmailRegistersAndWaitsForAdminApproval() throws Exception {
        String email = "fl01.google.newuser@gmail.com";
        assertThat(accounts.findByEmailIgnoreCase(email)).isEmpty();

        MockHttpServletResponse response = run(email, "Người Dùng Google");

        // Tạo tài khoản chờ duyệt, không giữ phiên đăng nhập
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?registered");
        Account created = accounts.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(AccountStatus.AWAITING_APPROVAL);
        assertThat(created.getFullName()).isEqualTo("Người Dùng Google");
        assertThat(sessionAuthorities()).isEmpty();

        // Đăng nhập Google lần nữa khi chưa được duyệt: vẫn chỉ báo đang chờ duyệt
        assertThat(run(email, "Người Dùng Google").getRedirectedUrl()).isEqualTo("/login?awaiting");
        assertThat(sessionAuthorities()).isEmpty();
    }

    @Test
    void existingAccountGetsItsRoleInSession() throws Exception {
        MockHttpServletResponse response = run("chau.le@cmscore.example", "Lê Minh Châu");
        // Tài khoản đã ACTIVE: vào hệ thống với đúng vai trò (Thư ký)
        assertThat(response.getRedirectedUrl()).isEqualTo("/dashboard");
        assertThat(sessionAuthorities()).contains("ROLE_EDITORIAL_SECRETARY");
    }

    @Test
    void newNonGmailIsRejected() throws Exception {
        MockHttpServletResponse response = run("someone@company.example", "X");
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=AUTH_REGISTRATION_FAILED");
        assertThat(accounts.findByEmailIgnoreCase("someone@company.example")).isEmpty();
    }

    private MockHttpServletRequest request;

    private MockHttpServletResponse run(String email, String name) throws Exception {
        var principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of("sub", "test-" + email, "email", email, "email_verified", true, "name", name), "sub");
        Authentication auth = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
        request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, auth);
        return response;
    }

    private List<String> sessionAuthorities() {
        Object saved = request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (!(saved instanceof SecurityContext context) || context.getAuthentication() == null) return List.of();
        return context.getAuthentication().getAuthorities().stream().map(a -> a.getAuthority()).toList();
    }
}
