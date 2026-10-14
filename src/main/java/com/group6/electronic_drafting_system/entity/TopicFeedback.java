package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.TopicFeedbackType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

// FL-01: mỗi vòng Trưởng phòng yêu cầu sửa (CHANGE_REQUEST) / lý do Thư ký từ chối (REJECTION) là một dòng; không sửa / xóa.
@Entity @Table(name = "topic_feedback", schema = "dbo") @Getter @Setter
public class TopicFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "topic_feedback_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_id", nullable = false) private Topic topic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false) private Account author;
    @Enumerated(EnumType.STRING) @Column(name = "feedback_type", nullable = false, length = 20) private TopicFeedbackType type;
    @Column(nullable = false, columnDefinition = "nvarchar(max)") private String content;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
