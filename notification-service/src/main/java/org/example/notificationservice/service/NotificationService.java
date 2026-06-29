package org.example.notificationservice.service;

import org.example.notificationservice.dto.response.NotificationResponse;
import org.example.notificationservice.dto.response.SimpleNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<SimpleNotificationResponse> findAllByUserId(Long userId, Pageable pageable);

    NotificationResponse markAsRead(String id, Long userId);

    void delete(String id, Long userId);
}
