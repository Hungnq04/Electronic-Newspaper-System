package com.group6.electronic_drafting_system.entity;

import com.group6.electronic_drafting_system.entity.enums.SourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

// Source / Material. INTERVIEW và INTERNAL_DOC chỉ dùng nội bộ; chỉ người thêm (added_by) được sửa / xóa.
@Entity @Table(name = "source", schema = "dbo") @Getter @Setter
public class Source {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "source_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "added_by", nullable = false) private Account addedBy;
    @Column(nullable = false, length = 255) private String title;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 15) private SourceType sourceType;
    @Column(name = "file_url", length = 500) private String fileUrl;
    @Column(columnDefinition = "nvarchar(max)") private String description;
    @Column(name = "uploaded_at", nullable = false, updatable = false) private OffsetDateTime uploadedAt;

    @PrePersist void onCreate() {
        if (uploadedAt == null) uploadedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
