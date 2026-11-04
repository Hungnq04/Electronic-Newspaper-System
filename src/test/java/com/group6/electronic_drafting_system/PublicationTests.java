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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// FL-07 Iter2: xuất bản ngay, hẹn giờ, job tự đăng. Iter3: trả bài về Lead, hủy lịch & trả bài.
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

    // Chạy trước mỗi test, trong cùng transaction của test (nên cũng tự hoàn tác, DB thật không đổi):
    // đưa A3, A4 về đúng dữ liệu mẫu, phòng khi đã bấm thử Đăng ngay / Hẹn giờ / Trả bài / Hủy lịch trên giao diện.
    // A4 = APPROVED, chưa có publication. A3 = SCHEDULED lúc 08:00 28/11/2026 (version 4, do bao.pham đặt lịch).
    @BeforeEach
    void resetDemoData() {
        // Bỏ các lượt "trả bài / hủy lịch" sinh ra khi bấm thử
        em.createNativeQuery("DELETE FROM article_review WHERE review_kind <> 'REVIEW' AND version_id IN "
                + "(SELECT version_id FROM article_version WHERE article_id IN (" + A3 + ", " + A4 + "))").executeUpdate();
        em.createNativeQuery("DELETE FROM publication WHERE article_id IN (" + A3 + ", " + A4 + ")").executeUpdate();
        em.createNativeQuery("UPDATE article SET status = 'APPROVED', completed_at = COALESCE(completed_at, '2026-10-06T16:40:00+07:00') "
                + "WHERE article_id = " + A4).executeUpdate();
        em.createNativeQuery("UPDATE article SET status = 'SCHEDULED', completed_at = COALESCE(completed_at, '2026-10-05T10:00:00+07:00') "
                + "WHERE article_id = " + A3).executeUpdate();
        em.createNativeQuery("INSERT INTO publication (article_id, version_id, published_by, publication_type, scheduled_at, published_at, status) "
                + "VALUES (" + A3 + ", 4, 2, 'SCHEDULED', '2026-11-28T08:00:00+07:00', NULL, 'SCHEDULED')").executeUpdate();
        em.clear();
    }

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

    // ------------------------------------------------------------ Iter3 - UC-28 Trả bài về Lead

    @Test
    void returnToLeadSendsApprovedArticleBack() {
        publicationService.returnToLead(A4, email("chau.le"), "  Thông tin đã cũ, cần cập nhật số liệu.  ");
        em.flush();
        em.clear();

        Article a4 = article(A4);
        assertThat(a4.getStatus()).isEqualTo(ArticleStatus.REVISION_REQUIRED);
        assertThat(a4.getCompletedAt()).isNull();                         // ck_article_completed
        assertThat(publications.findByArticle_Id(A4)).isEmpty();
        // Có một lượt Thư ký RETURN_TO_LEAD, lý do đã bỏ khoảng trắng thừa
        assertThat(returnReviews(A4, "RETURN_TO_LEAD")).containsExactly("Thông tin đã cũ, cần cập nhật số liệu.");
        assertThat(ruleViolations()).isZero();
    }

    @Test
    void returnToLeadRequiresReason() {
        assertError(() -> publicationService.returnToLead(A4, email("chau.le"), ""), ErrorCode.PUBLICATION_REASON_REQUIRED);
        assertError(() -> publicationService.returnToLead(A4, email("chau.le"), "   "), ErrorCode.PUBLICATION_REASON_REQUIRED);
        assertError(() -> publicationService.returnToLead(A4, email("chau.le"), null), ErrorCode.PUBLICATION_REASON_REQUIRED);
        assertThat(article(A4).getStatus()).isEqualTo(ArticleStatus.APPROVED);
    }

    @Test
    void returnToLeadOnlyForApprovedArticle() {
        // Bài đã hẹn giờ phải dùng Cancel & Return; bài đã đăng không gỡ được (BR-33)
        assertError(() -> publicationService.returnToLead(A3, email("chau.le"), "Lý do"), ErrorCode.PUBLICATION_NOT_APPROVED);
        assertError(() -> publicationService.returnToLead(A1, email("chau.le"), "Lý do"), ErrorCode.PUBLICATION_NOT_APPROVED);
        assertError(() -> publicationService.returnToLead(A4, email("mai.vu"), "Lý do"), ErrorCode.PUBLICATION_ACCESS_DENIED);
    }

    @Test
    void returnToLeadWithEmptyReasonReopensDialog() throws Exception {
        mvc.perform(post("/secretary/publications/" + A4 + "/return").param("reason", " ").with(csrf())
                        .with(user(email("chau.le")).roles("EDITORIAL_SECRETARY")))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/secretary/publications/" + A4))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", ErrorCode.PUBLICATION_REASON_REQUIRED.getMessage()));
        assertThat(article(A4).getStatus()).isEqualTo(ArticleStatus.APPROVED);
    }

    // ------------------------------------------------------------ Iter3 - Màn Scheduled Publication List / Detail

    @Test
    void secretarySeesScheduledArticleInList() throws Exception {
        mvc.perform(get("/secretary/scheduled-publications").with(user(email("chau.le")).roles("EDITORIAL_SECRETARY")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/secretary/scheduled-publications/" + A3)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/secretary/scheduled-publications/" + A4 + "\""))));
    }

    @Test
    void secretaryOpensScheduledDetailWithCancelDialog() throws Exception {
        mvc.perform(get("/secretary/scheduled-publications/" + A3).with(user(email("chau.le")).roles("EDITORIAL_SECRETARY")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cancel-dialog")));
        // Bài không hẹn giờ (A4 đang APPROVED) thì không mở được màn này, quay về danh sách
        mvc.perform(get("/secretary/scheduled-publications/" + A4).with(user(email("chau.le")).roles("EDITORIAL_SECRETARY")))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/secretary/scheduled-publications"));
    }

    @Test
    void reporterCannotCancelSchedule() throws Exception {
        mvc.perform(get("/secretary/scheduled-publications").with(user(email("mai.vu")).roles("REPORTER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/secretary/scheduled-publications/" + A3 + "/cancel").param("reason", "x").with(csrf())
                        .with(user(email("mai.vu")).roles("REPORTER")))
                .andExpect(status().isForbidden());
        assertError(() -> publicationService.cancelAndReturn(A3, email("mai.vu"), "x"), ErrorCode.PUBLICATION_ACCESS_DENIED);
        assertThat(article(A3).getStatus()).isEqualTo(ArticleStatus.SCHEDULED);
    }

    // ------------------------------------------------------------ Iter3 - UC-31 Hủy lịch & trả bài về Lead

    @Test
    void cancelAndReturnDeletesScheduleAndReturnsArticle() {
        publicationService.cancelAndReturn(A3, email("chau.le"), "Sự kiện bị hoãn, chưa đăng được.");
        em.flush();
        em.clear();

        Article a3 = article(A3);
        assertThat(a3.getStatus()).isEqualTo(ArticleStatus.REVISION_REQUIRED);
        assertThat(a3.getCompletedAt()).isNull();
        assertThat(publications.findByArticle_Id(A3)).isEmpty();          // BR-31: xóa dòng publication
        assertThat(returnReviews(A3, "CANCEL_SCHEDULE")).containsExactly("Sự kiện bị hoãn, chưa đăng được.");
        // Job không còn thấy bài này
        assertThat(publicationService.findDueScheduledArticleIds()).doesNotContain(A3);
        assertThat(ruleViolations()).isZero();
    }

    @Test
    void cancelRequiresReason() {
        assertError(() -> publicationService.cancelAndReturn(A3, email("chau.le"), " "), ErrorCode.PUBLICATION_REASON_REQUIRED);
        assertThat(article(A3).getStatus()).isEqualTo(ArticleStatus.SCHEDULED);
        assertThat(publications.findByArticle_Id(A3)).isPresent();
    }

    @Test
    void cannotCancelAfterScheduledTime() {
        // Đã đến giờ (job chưa kịp chạy): không được hủy nữa
        moveScheduleToPast(A3);
        assertError(() -> publicationService.cancelAndReturn(A3, email("chau.le"), "Lý do"), ErrorCode.PUBLICATION_SCHEDULE_ALREADY_DUE);
        assertThat(article(A3).getStatus()).isEqualTo(ArticleStatus.SCHEDULED);
    }

    @Test
    void cannotCancelArticleThatIsNotScheduled() {
        assertError(() -> publicationService.cancelAndReturn(A4, email("chau.le"), "Lý do"), ErrorCode.PUBLICATION_NOT_SCHEDULED);
        assertError(() -> publicationService.cancelAndReturn(A1, email("chau.le"), "Lý do"), ErrorCode.PUBLICATION_NOT_SCHEDULED);
    }

    @Test
    void cancelThroughWebRedirectsToScheduledList() throws Exception {
        mvc.perform(post("/secretary/scheduled-publications/" + A3 + "/cancel").param("reason", "Hoãn đăng").with(csrf())
                        .with(user(email("chau.le")).roles("EDITORIAL_SECRETARY")))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/secretary/scheduled-publications"));
        assertThat(article(A3).getStatus()).isEqualTo(ArticleStatus.REVISION_REQUIRED);
    }

    // ------------------------------------------------------------ Hàm phụ

    // Lý do của các lượt Thư ký trả bài / hủy lịch (review_kind) trên mọi version của bài
    @SuppressWarnings("unchecked")
    private java.util.List<String> returnReviews(int articleId, String reviewKind) {
        return em.createNativeQuery("""
                        SELECT r.feedback FROM article_review r JOIN article_version v ON v.version_id = r.version_id
                        WHERE v.article_id = ?1 AND r.review_kind = ?2
                          AND r.review_stage = 'SECRETARY' AND r.decision = 'REQUEST_REVISION' AND r.reviewed_at IS NOT NULL""")
                .setParameter(1, articleId).setParameter(2, reviewKind).getResultList();
    }

    private String email(String username) {
        return (String) em.createNativeQuery("SELECT email FROM account WHERE username = ?1")
                .setParameter(1, username).getSingleResult();
    }

    private Article article(int id) {
        em.flush();   // ghi các thay đổi đang chờ xuống DB trước, rồi mới xóa bộ nhớ tạm để đọc lại từ DB
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
