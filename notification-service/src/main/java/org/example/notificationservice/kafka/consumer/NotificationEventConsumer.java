package org.example.notificationservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.UserCreatedEvent;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.repository.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = KafkaTopics.USER_CREATED_TOPIC)
    public void handleCreatedUser(UserCreatedEvent event){
        Notification notification = Notification.builder()
                .userId(event.getId())
                .username(event.getUsername())
                .title("Welcome!")
                .message("Welcome to booking events service")
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }
}
