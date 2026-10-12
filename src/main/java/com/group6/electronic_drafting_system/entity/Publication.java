package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.PublicationStatus;
import com.group6.electronic_drafting_system.entity.enums.PublicationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

// Mỗi bài tối đa một publication; hủy lịch đăng thì xóa dòng. version ghim đúng bản được Final Approve.
@Entity @Table(name = "publication", schema = "dbo") @Getter @Setter
public class Publication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "publication_id") private Integer id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "article_id", nullable = false, unique = true) private Article article;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "version_id", nullable = false) private ArticleVersion version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "published_by", nullable = false) private Account publishedBy;
    @Enumerated(EnumType.STRING) @Column(name = "publication_type", nullable = false, length = 10) private PublicationType publicationType;
    @Column(name = "scheduled_at") private OffsetDateTime scheduledAt;
    @Column(name = "published_at") private OffsetDateTime publishedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private PublicationStatus status;
}
