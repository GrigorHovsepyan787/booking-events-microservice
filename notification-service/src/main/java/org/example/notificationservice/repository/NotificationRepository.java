package org.example.notificationservice.repository;

import org.example.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByUserId(Long userId, Pageable pageable);

    void deleteAllByUserId(Long userId);
}
