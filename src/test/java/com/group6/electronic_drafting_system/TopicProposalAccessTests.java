package com.group6.electronic_drafting_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.electronic_drafting_system.dto.Topic.TopicCreateRequestDTO;
import com.group6.electronic_drafting_system.entity.enums.AccountStatus;
import com.group6.electronic_drafting_system.entity.enums.RoleName;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.service.TopicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// FL-01 Iter1: chỉ REPORTER được vào / tạo Topic (chặn ở URL lẫn service); pop-up Login, Logout, điều hướng sau đăng nhập.
// Cần DB cms_core đã chạy database/fl01_iter1_migration.sql. Không ghi gì vào DB.
@SpringBootTest
@AutoConfigureMockMvc
class TopicProposalAccessTests {
    @Autowired MockMvc mvc;
    @Autowired TopicService topicService;
    @Autowired AccountRepository accounts;

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/reporter/topics")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/reporter/topics").with(csrf()).param("title", "x"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "DEPARTMENT_HEAD")
    void departmentHeadCannotOpenOrCreate() throws Exception {
        mvc.perform(get("/reporter/topics")).andExpect(status().isForbidden());
        mvc.perform(get("/reporter/topics/new")).andExpect(status().isForbidden());
        mvc.perform(post("/reporter/topics").with(csrf()).param("title", "x").param("summary", "y").param("departmentId", "4"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EDITORIAL_SECRETARY")
    void secretaryCannotCreate() throws Exception {
        mvc.perform(post("/reporter/topics").with(csrf()).param("title", "x").param("summary", "y").param("departmentId", "4"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REPORTER")
    void reporterLandsOnTopicListAfterLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/reporter/topics"));
    }

    @Test
    @WithMockUser(roles = "DEPARTMENT_HEAD")
    void departmentHeadLandsOnDashboard() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/department-head/dashboard"));
    }

    @Test
    @WithMockUser(roles = "EDITORIAL_SECRETARY")
    void secretaryLandsOnFinalReviews() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/secretary/final-reviews"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminLandsOnAccountList() throws Exception {
        // TV4 Iter3: Admin đã có màn quản trị tài khoản
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/accounts"));
    }

    @Test
    void loginIsPopupAndLogoutEndsSession() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("role=\"dialog\"")));
        mvc.perform(post("/logout").with(csrf()).with(user("x").roles("REPORTER")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?logout"));
    }

    // Lớp thứ hai: gọi thẳng service bằng tài khoản Trưởng ban / Thư ký thật trong DB cũng bị từ chối
    @Test
    @Transactional // giữ session để đọc role; rollback nên không ghi gì
    void serviceRejectsNonReporterAccounts() {
        TopicCreateRequestDTO request = new TopicCreateRequestDTO();
        request.setTitle("x");
        request.setSummary("y");
        request.setDepartmentId(4);
        for (RoleName role : new RoleName[] { RoleName.DEPARTMENT_HEAD, RoleName.EDITORIAL_SECRETARY }) {
            String email = accounts.findAll().stream()
                    .filter(a -> a.getRole().getRoleName() == role && a.getStatus() == AccountStatus.ACTIVE)
                    .map(a -> a.getEmail()).findFirst().orElseThrow();
            assertThatThrownBy(() -> topicService.createProposal(email, request))
                    .isInstanceOf(ApplicationException.class)
                    .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode()).isEqualTo(ErrorCode.TOPIC_CREATE_FORBIDDEN));
        }
    }
}
