package org.example.notificationservice.service;

import org.example.notificationservice.dto.NotificationDto;
import org.example.notificationservice.dto.SimpleNotificationDto;

import java.util.List;

public interface NotificationService {
    List<SimpleNotificationDto> findAllByUserId(Long userId);

    NotificationDto markAsRead(Long id, Long userId);

    void delete(Long id, Long userId);
}
