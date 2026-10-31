package com.group6.electronic_drafting_system.service;

import com.group6.electronic_drafting_system.dto.Topic.NotificationItemDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Topic;
import java.util.List;

// FL-01 Iter2: thông báo trong hệ thống cho phóng viên (chuông trên header)
public interface NotificationService {
    // Tạo một thông báo (gọi bên trong transaction của thao tác duyệt)
    void notify(Account recipient, Topic topic, String message);
    long countUnread(Integer accountId);
    List<NotificationItemDTO> getRecent(Integer accountId);
    // Đánh dấu đã đọc, trả về topic_id để mở đề tài
    Integer open(String email, Integer notificationId);
}
