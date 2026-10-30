package com.group6.electronic_drafting_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.electronic_drafting_system.entity.Article;
import com.group6.electronic_drafting_system.entity.Publication;
import com.group6.electronic_drafting_system.entity.enums.ArticleStatus;
import com.group6.electronic_drafting_system.entity.enums.PublicationStatus;
import com.group6.electronic_drafting_system.entity.enums.PublicationType;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.ArticleRepository;
import com.group6.electronic_drafting_system.repository.PublicArticleRepository;
import com.group6.electronic_drafting_system.repository.PublicationRepository;
import com.group6.electronic_drafting_system.service.PublicationService;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// FL-07 Iter2: xuất bản ngay, hẹn giờ, job tự đăng.
// @Transactional: mỗi test tự hoàn tác (rollback) khi chạy xong, dữ liệu mẫu không bị đổi.
// Dữ liệu mẫu dùng: A4 = APPROVED (chờ đăng), A3 = SCHEDULED 28/11/2026, A5 = DEPARTMENT_APPROVED, A1 = PUBLISHED.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicationTests {
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final int A1 = 1, A3 = 3, A4 = 4, A5 = 5;

    @Autowired MockMvc mvc;
    @Autowired PublicationService publicationService;
    @Autowired ArticleRepository articles;
    @Autowired PublicationRepository publications;
    @Autowired PublicArticleRepository publicArticles;
    @Autowired EntityManager em;

    // ------------------------------------------------------------ Quyền truy cập màn hình

    @Test
    void secretarySeesApprovedArticleInList() throws Exception {
        mvc.perform(get("/secretary/publications").with(user(email("chau.le")).roles("EDITORIAL_SECRETARY")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/secretary/publications/" + A4)));
    }

    @Test
    void secretaryOpensPublicationDetail() throws Exception {
        mvc.perform(get("/secretary/publications/" + A4).with(user(email("chau.le")).roles("EDITORIAL_SECRETARY")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("publish-dialog")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("schedule-dialog")));
    }

    @Test
    void reporterCannotOpenOrPublish() throws Exception {
        mvc.perform(get("/secretary/publications").with(user(email("mai.vu")).roles("REPORTER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/secretary/publications/" + A4 + "/publish").with(csrf()).with(user(email("mai.vu")).roles("REPORTER")))
                .andExpect(status().isForbidden());
        assertThat(article(A4).getStatus()).isEqualTo(ArticleStatus.APPROVED);
    }

    @Test
    void serviceRejectsNonSecretaryEvenWithoutWebLayer() {
        // Kiểm tra quyền ở backend: gọi thẳng service bằng tài khoản Reporter vẫn bị chặn
        assertError(() -> publicationService.publishNow(A4, email("mai.vu")), ErrorCode.PUBLICATION_ACCESS_DENIED);
    }

    // ------------------------------------------------------------ UC-29 Đăng ngay

    @Test
    void publishNowMakesArticlePublic() {
        publicationService.publishNow(A4, email("chau.le"));
        em.flush();
        em.clear();

        assertThat(article(A4).getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        Publication p = publications.findByArticle_Id(A4).orElseThrow();
        assertThat(p.getPublicationType()).isEqualTo(PublicationType.IMMEDIATE);
        assertThat(p.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(p.getPublishedAt()).isNotNull();
        assertThat(p.getScheduledAt()).isNull();
        // Trang công khai (view v_public_article) thấy bài ngay
        assertThat(publicArticles.findById(A4)).isPresent();
        assertThat(ruleViolations()).isZero();
    }

    @Test
    void cannotPublishArticleThatIsNotApproved() {
        assertError(() -> publicationService.publishNow(A5, email("chau.le")), ErrorCode.PUBLICATION_NOT_APPROVED);
        assertError(() -> publicationService.publishNow(A1, email("chau.le")), ErrorCode.PUBLICATION_NOT_APPROVED);
    }

    @Test
    void cannotPublishTheSameArticleTwice() {
        publicationService.publishNow(A4, email("chau.le"));
        assertError(() -> publicationService.publishNow(A4, email("bao.pham")), ErrorCode.PUBLICATION_NOT_APPROVED);
        assertError(() -> publicationService.schedule(A4, email("bao.pham"), inMinutes(60)), ErrorCode.PUBLICATION_NOT_APPROVED);
    }

    // ------------------------------------------------------------ UC-30 Hẹn giờ

    @Test
    void scheduleCreatesScheduledPublication() {
        publicationService.schedule(A4, email("chau.le"), inMinutes(60));
        em.flush();
        em.clear();

        assertThat(article(A4).getStatus()).isEqualTo(ArticleStatus.SCHEDULED);
        Publication p = publications.findByArticle_Id(A4).orElseThrow();
        assertThat(p.getPublicationType()).isEqualTo(PublicationType.SCHEDULED);
        assertThat(p.getStatus()).isEqualTo(PublicationStatus.SCHEDULED);
        assertThat(p.getScheduledAt()).isNotNull();
        assertThat(p.getPublishedAt()).isNull();
        // Chưa đến giờ: chưa xuất hiện trên trang công khai
        assertThat(publicArticles.findById(A4)).isEmpty();
        assertThat(ruleViolations()).isZero();
    }

    @Test
    void scheduleRejectsTimeTooSoonOrInvalid() {
        assertError(() -> publicationService.schedule(A4, email("chau.le"), inMinutes(2)), ErrorCode.PUBLICATION_SCHEDULE_TIME_TOO_SOON);
        assertError(() -> publicationService.schedule(A4, email("chau.le"), inMinutes(-60)), ErrorCode.PUBLICATION_SCHEDULE_TIME_TOO_SOON);
        assertError(() -> publicationService.schedule(A4, email("chau.le"), ""), ErrorCode.PUBLICATION_SCHEDULE_TIME_REQUIRED);
        assertError(() -> publicationService.schedule(A4, email("chau.le"), "abc"), ErrorCode.PUBLICATION_SCHEDULE_TIME_REQUIRED);
        // Bị từ chối thì bài vẫn chờ đăng, không có publication
        assertThat(article(A4).getStatus()).isEqualTo(ArticleStatus.APPROVED);
        assertThat(publications.findByArticle_Id(A4)).isEmpty();
    }

    // ------------------------------------------------------------ UC-32 Job tự đăng

    @Test
    void jobPublishesScheduledArticleWhenTimeComes() {
        publicationService.schedule(A4, email("chau.le"), inMinutes(60));
        em.flush();
        // Giả lập "đã đến giờ": dời giờ hẹn về 1 phút trước
        moveScheduleToPast(A4);

        assertThat(publicationService.findDueScheduledArticleIds()).contains(A4).doesNotContain(A3);
        assertThat(publicationService.publishScheduled(A4)).isTrue();
        em.flush();
        em.clear();

        assertThat(article(A4).getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        Publication p = publications.findByArticle_Id(A4).orElseThrow();
        assertThat(p.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(p.getPublishedAt()).isAfterOrEqualTo(p.getScheduledAt());
        assertThat(publicArticles.findById(A4)).isPresent();
        assertThat(ruleViolations()).isZero();
    }

    @Test
    void jobDoesNotPublishBeforeScheduledTime() {
        // A3 hẹn ngày 28/11/2026: chưa đến giờ thì không đăng
        assertThat(publicationService.publishScheduled(A3)).isFalse();
        assertThat(article(A3).getStatus()).isEqualTo(ArticleStatus.SCHEDULED);
    }

    @Test
    void jobSkipsArticleThatIsNoLongerScheduled() {
        // Giả lập: lịch đã đến giờ nhưng bài không còn SCHEDULED -> job phải bỏ qua
        moveScheduleToPast(A3);
        em.createNativeQuery("UPDATE article SET status = 'APPROVED' WHERE article_id = " + A3).executeUpdate();
        em.clear();

        assertThat(publicationService.publishScheduled(A3)).isFalse();
        assertThat(publications.findByArticle_Id(A3).orElseThrow().getStatus()).isEqualTo(PublicationStatus.SCHEDULED);
    }

    // ------------------------------------------------------------ Hàm phụ

    private String email(String username) {
        return (String) em.createNativeQuery("SELECT email FROM account WHERE username = ?1")
                .setParameter(1, username).getSingleResult();
    }

    private Article article(int id) {
        em.clear();
        return articles.findById(id).orElseThrow();
    }

    private static String inMinutes(int minutes) {
        return OffsetDateTime.now(ZoneOffset.ofHours(7)).plusMinutes(minutes).format(INPUT);
    }

    private void moveScheduleToPast(int articleId) {
        em.flush();
        em.createNativeQuery("UPDATE publication SET scheduled_at = DATEADD(MINUTE, -1, SYSDATETIMEOFFSET()) WHERE article_id = " + articleId)
                .executeUpdate();
        em.clear();
    }

    private int ruleViolations() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM v_rule_violations").getSingleResult()).intValue();
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getErrorCode()).isEqualTo(expected);
    }
}
