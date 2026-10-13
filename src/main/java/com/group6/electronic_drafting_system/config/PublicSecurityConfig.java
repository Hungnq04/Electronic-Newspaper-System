package com.group6.electronic_drafting_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

// FL-07 trang công khai (UC-33, UC-34): độc giả xem không cần đăng nhập.
// Tách khỏi SecurityConfig để không đụng code chung; chỉ khớp GET đúng các URL dưới đây,
// mọi request khác (kể cả POST /logout) vẫn đi qua chuỗi trong SecurityConfig.
@Configuration
public class PublicSecurityConfig {

    @Bean @Order(1)
    SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher.Builder get = PathPatternRequestMatcher.withDefaults();
        return http
                .securityMatcher(new OrRequestMatcher(
                        get.matcher(HttpMethod.GET, "/"),
                        get.matcher(HttpMethod.GET, "/news"),
                        get.matcher(HttpMethod.GET, "/news/{id}"),
                        get.matcher(HttpMethod.GET, "/uploads/**")))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
