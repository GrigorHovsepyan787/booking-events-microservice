package org.example.notificationservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.response.NotificationResponse;
import org.example.notificationservice.dto.response.SimpleNotificationResponse;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.exception.NotificationNotFoundException;
import org.example.notificationservice.mapper.NotificationMapper;
import org.example.notificationservice.repository.NotificationRepository;
import org.example.notificationservice.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public Page<SimpleNotificationResponse> findAllByUserId(Long userId, Pageable pageable) {
        log.info("Fetching all notifications for user ID: {}", userId);
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);
        log.debug("Found {} notifications for user ID: {}", notifications.getTotalElements(), userId);
        return notifications.map(notificationMapper::toSimpleDto);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String id, Long userId) {
        log.info("Attempting to mark notification ID: {} as read by user ID: {}", id, userId);
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> {
            log.warn("Notification not found with ID: {}", id);
            return new NotificationNotFoundException(id);
        });
        notification.setRead(true);
        log.debug("Validating ownership for notification ID: {} and user ID: {}", id, userId);
        validateUser(notification, userId);
        Notification saved = notificationRepository.save(notification);
        log.info("Notification ID: {} successfully marked as read and saved", id);
        return notificationMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(String id, Long userId) {
        log.info("Attempting to delete notification ID: {} by user ID: {}", id, userId);
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
        validateUser(notification, userId);
        notificationRepository.deleteById(id);
        log.info("Notification ID: {} successfully deleted by user ID: {}", id, userId);
    }

    private void validateUser(Notification notification, Long userId) {
        if (!notification.getUserId().equals(userId)) {
            log.error("Access denied: User ID {} is not allowed to modify notification ID {} owned by User ID {}",
                    userId, notification.getId(), notification.getUserId());
            throw new AccessDeniedException("Not allowed for actions");
        }
    }
}
