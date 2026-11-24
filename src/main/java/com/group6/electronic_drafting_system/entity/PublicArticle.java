package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import com.group6.electronic_drafting_system.dto.DepartmentHead.ContentBlockDTO;
import com.group6.electronic_drafting_system.mapper.DepartmentHead.ReviewWorkspaceMapper;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

// Đọc từ view v_public_article: chỉ bài PUBLISHED, nội dung lấy từ đúng version đã Final Approve, không có tài liệu nội bộ.
@Entity @Immutable @Table(name = "v_public_article", schema = "dbo") @Getter
public class PublicArticle {
    public static final ZoneOffset VN_OFFSET = ZoneOffset.ofHours(7);

    @Id @Column(name = "article_id") private Integer articleId;
    @Column(name = "version_id") private Integer versionId;
    private String title;
    private String subtitle;
    private String sapo;
    private String content;
    private String category;
    @Column(name = "published_at") private OffsetDateTime publishedAt;
    private String author;
    @Column(name = "featured_image_url") private String featuredImageUrl;
    @Column(name = "featured_image_caption") private String featuredImageCaption;

    public OffsetDateTime getPublishedAtVn() {
        return publishedAt == null ? null : publishedAt.withOffsetSameInstant(VN_OFFSET);
    }

    // Thân bài tách thành khối (đoạn chữ, ảnh, video, bảng). Hiểu cả HTML của trình soạn thảo lẫn chữ thường (dữ liệu mẫu cũ).
    public List<ContentBlockDTO> getBlocks() {
        return new ReviewWorkspaceMapper().blocks(content);
    }
}
