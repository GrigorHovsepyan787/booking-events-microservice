package org.example.notificationservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.NotificationDto;
import org.example.notificationservice.dto.SimpleNotificationDto;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.mapper.NotificationMapper;
import org.example.notificationservice.repository.NotificationRepository;
import org.example.notificationservice.service.NotificationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public List<SimpleNotificationDto> findAllByUserId(Long userId) {
        return notificationRepository.findByUserId(userId).stream().map(notificationMapper::toSimpleDto).toList();
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        notification.setRead(true);
        validateUser(notification, userId);
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        validateUser(notification, userId);
        notificationRepository.deleteById(id);
    }

    private void validateUser(Notification notification, Long userId) {
        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("Not allowed for actions");
        }
    }
}
