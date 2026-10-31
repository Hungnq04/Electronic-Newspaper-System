package com.group6.electronic_drafting_system.service.impl;

import com.group6.electronic_drafting_system.dto.Topic.NotificationItemDTO;
import com.group6.electronic_drafting_system.entity.Account;
import com.group6.electronic_drafting_system.entity.Notification;
import com.group6.electronic_drafting_system.entity.Topic;
import com.group6.electronic_drafting_system.exception.ApplicationException;
import com.group6.electronic_drafting_system.exception.ErrorCode;
import com.group6.electronic_drafting_system.repository.AccountRepository;
import com.group6.electronic_drafting_system.repository.NotificationRepository;
import com.group6.electronic_drafting_system.service.NotificationService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final int MESSAGE_MAX = 500;
    // Chuông chỉ hiện các thông báo gần nhất
    private static final int RECENT_LIMIT = 20;

    private final NotificationRepository notifications;
    private final AccountRepository accounts;

    public NotificationServiceImpl(NotificationRepository notifications, AccountRepository accounts) {
        this.notifications = notifications;
        this.accounts = accounts;
    }

    @Override
    @Transactional
    public void notify(Account recipient, Topic topic, String message) {
        Notification n = new Notification();
        n.setAccount(recipient);
        n.setTopic(topic);
        n.setMessage(message.length() > MESSAGE_MAX ? message.substring(0, MESSAGE_MAX) : message);
        notifications.save(n);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Integer accountId) {
        return notifications.countByAccount_IdAndReadFalse(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationItemDTO> getRecent(Integer accountId) {
        return notifications.findByAccount_IdOrderByCreatedAtDescIdDesc(accountId).stream()
                .limit(RECENT_LIMIT)
                .map(n -> new NotificationItemDTO(n.getId(), n.getTopic().getId(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public Integer open(String email, Integer notificationId) {
        Account me = email == null ? null : accounts.findByEmailIgnoreCase(email.trim()).orElse(null);
        Notification n = notifications.findById(notificationId).orElse(null);
        // Chỉ người nhận được mở thông báo của mình; của người khác thì coi như không tồn tại
        if (me == null || n == null || !n.getAccount().getId().equals(me.getId())) {
            throw new ApplicationException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        n.setRead(true);
        return n.getTopic().getId();
    }
}
