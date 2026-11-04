package com.group6.electronic_drafting_system.job;

import com.group6.electronic_drafting_system.service.PublicationService;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Màn #70 Scheduled Article Publisher (UC-32): tự xuất bản bài hẹn giờ khi đến giờ.
// Không ai bấm nút: Spring tự gọi method run() mỗi 60 giây.
@Component
public class ScheduledPublicationJob {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScheduledPublicationJob.class);

    private final PublicationService publicationService;

    public ScheduledPublicationJob(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    // initialDelay: chờ 10 giây sau khi app khởi động (bài đã quá giờ lúc app tắt sẽ được đăng ngay lần chạy đầu)
    // fixedDelay: chạy xong lần trước thì 60 giây sau chạy lần tiếp theo
    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public void run() {
        List<Integer> dueArticleIds = publicationService.findDueScheduledArticleIds();
        for (Integer articleId : dueArticleIds) {
            try {
                // Mỗi bài một transaction; service tự kiểm tra bài VẪN CÒN SCHEDULED rồi mới đăng
                if (publicationService.publishScheduled(articleId)) {
                    log.info("Auto-published scheduled article {}", articleId);
                }
            } catch (Exception ex) {
                // Một bài lỗi thì ghi log và đăng tiếp các bài còn lại
                log.error("Auto-publish failed for article {}", articleId, ex);
            }
        }
    }
}
