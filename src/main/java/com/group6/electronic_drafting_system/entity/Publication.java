package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "PUBLICATION", schema = "dbo") @Getter @Setter
public class Publication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "publication_id") private Integer id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "version_id", nullable = false, unique = true) private ArticleVersion version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "published_by", nullable = false) private Account publishedBy;
    @Column(name = "publication_type", nullable = false, length = 20) private String publicationType;
    @Column(name = "scheduled_at") private LocalDateTime scheduledAt;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(nullable = false, length = 20) private String status = "Scheduled";
}
