package com.group6.electronic_drafting_system.config;

import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

@Configuration
public class SecurityConfig {
    public static final String CSRF_EXPIRED_ATTR = "CSRF_EXPIRED";

    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(AccountRepository accounts) {
        return email -> accounts.findByEmailIgnoreCase(email.trim())
                .map(account -> User.withUsername(account.getEmail().toLowerCase(Locale.ROOT))
                        .password(account.getPasswordHash()).roles(account.getRole().getRoleName().name())
                        .accountLocked(account.getStatus() == AccountStatus.LOCKED)
                        .disabled(account.getStatus() != AccountStatus.ACTIVE).build())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, GoogleLoginSuccessHandler googleSuccessHandler) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/register/**", "/css/**", "/error", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/process-login").usernameParameter("email").defaultSuccessUrl("/", true).permitAll())
                .oauth2Login(oauth -> oauth.loginPage("/login").successHandler(googleSuccessHandler))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                // Token CSRF hết hạn (session cũ mất sau khi restart app / để trang mở quá lâu): quay lại trang trước thay vì trang 403
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, denied) -> {
                    if (denied instanceof CsrfException) {
                        String referer = request.getHeader("Referer");
                        String target = referer != null && referer.startsWith(request.getScheme() + "://" + request.getHeader("Host") + "/")
                                ? referer : request.getContextPath() + "/login";
                        request.getSession(true).setAttribute(CSRF_EXPIRED_ATTR, Boolean.TRUE);
                        response.sendRedirect(target);
                    } else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    }
                }))
                .build();
    }
}
