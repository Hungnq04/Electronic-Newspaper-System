package com.group6.electronic_drafting_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "MATERIAL", schema = "dbo") @Getter @Setter
public class Material {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "material_id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "uploaded_by", nullable = false) private Account uploadedBy;
    @Column(name = "material_name", nullable = false, length = 255) private String materialName;
    @Column(name = "material_type", nullable = false, length = 30) private String materialType;
    @Column(name = "storage_url", nullable = false, length = 500) private String storageUrl;
    @Column(name = "file_size") private Long fileSize;
    @Column(name = "mime_type", length = 100) private String mimeType;
    @Column(length = 255) private String description;
    @Column(name = "uploaded_at", nullable = false, insertable = false, updatable = false) private LocalDateTime uploadedAt;
    @ManyToMany(mappedBy = "materials") private List<Topic> topics;
    @ManyToMany(mappedBy = "materials") private List<ArticleVersion> versions;
}
