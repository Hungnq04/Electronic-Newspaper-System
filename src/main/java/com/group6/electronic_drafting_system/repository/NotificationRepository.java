package com.group6.electronic_drafting_system.repository;

import com.group6.electronic_drafting_system.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    // Số thông báo chưa đọc (con số trên chuông)
    long countByAccount_IdAndReadFalse(Integer accountId);

    // Danh sách thông báo của một người, mới nhất trước
    @EntityGraph(attributePaths = "topic")
    List<Notification> findByAccount_IdOrderByCreatedAtDescIdDesc(Integer accountId);
}
