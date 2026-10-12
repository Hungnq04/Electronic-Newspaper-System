package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Entity @Table(name = "topic_source", schema = "dbo", uniqueConstraints = @UniqueConstraint(name = "uq_topic_source", columnNames = {"topic_id", "source_id"})) @Getter @Setter
public class TopicSource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "topic_source_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_id", nullable = false) private Topic topic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "source_id", nullable = false) private Source source;
    @Column(name = "attached_at", nullable = false, updatable = false) private OffsetDateTime attachedAt;
    @Column(columnDefinition = "nvarchar(max)") private String note;

    @PrePersist void onCreate() {
        if (attachedAt == null) attachedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
