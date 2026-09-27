package com.group6.electronic_drafting_system.config;

import com.group6.electronic_drafting_system.repository.AccountRepository;
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

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(AccountRepository accounts) {
        return email -> accounts.findByEmailIgnoreCase(email.trim())
                .map(account -> User.withUsername(account.getEmail().toLowerCase(Locale.ROOT))
                        .password(account.getPasswordHash()).roles(account.getRole().getRoleName())
                        .disabled(!"Active".equalsIgnoreCase(account.getStatus())).build())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, GoogleLoginSuccessHandler googleSuccessHandler) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/register/**", "/css/**", "/error", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/", true).permitAll())
                .oauth2Login(oauth -> oauth.loginPage("/login").successHandler(googleSuccessHandler))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }
}
