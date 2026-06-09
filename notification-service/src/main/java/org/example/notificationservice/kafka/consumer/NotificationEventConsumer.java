package org.example.notificationservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.BookingCancelledEvent;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.BookingDeletedEvent;
import org.example.common.kafka.event.EventCreated;
import org.example.common.kafka.event.EventUpdated;
import org.example.common.kafka.event.UserCreatedEvent;
import org.example.common.kafka.event.UserDeletedEvent;
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

    @KafkaListener(topics = KafkaTopics.USER_DELETED_TOPIC)
    public void handleUserDeleted(UserDeletedEvent event){
        notificationRepository.deleteAllByUserId(event.getUserId());
    }

    @KafkaListener(topics = KafkaTopics.EVENT_CREATED_TOPIC)
    public void handleCreatedEvent(EventCreated event){
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .title("Event was successfully created!")
                .message(event.getTitle() + " event has been created," +
                        " event date - " + event.getEventDate() +
                        ". With event location - " + event.getLocation())
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @KafkaListener(topics = KafkaTopics.EVENT_UPDATED_TOPIC)
    public void handleUpdatedEvent(EventUpdated event){
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .title("Event was successfully updated!")
                .message(event.getTitle() + " event has been updated," +
                        " new event date - " + event.getEventDate() +
                        ". With event new location - " + event.getLocation())
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_APPROVED_TOPIC)
    public void handleApproved(BookingCreatedEvent event){
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .title("Booking was successfully approved!")
                .message("Booking for event with id -" + event.getEventId() + " has been approved.")
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CANCELLED_TOPIC)
    public void handleCancelled(BookingCancelledEvent event){
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .title("Booking has been cancelled!")
                .message("Booking for event with id -" + event.getEventId() + " has been cancelled.")
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CREATED_TOPIC)
    public void handleBookingCreated(BookingCreatedEvent event){
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .title("Booking has been created!")
                .message("Booking for event with id -" + event.getEventId() + " is pending now.")
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_DELETED_TOPIC)
    public void handleBookingDeleted(BookingDeletedEvent event){
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .title("Booking successfully deleted!")
                .message("Booking for event with id -" + event.getEventId() + " successfully deleted.")
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }
}
