package org.example.notificationservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.example.common.correlation.CorrelationConstants;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.BookingCancelledEvent;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.BookingDeletedEvent;
import org.example.common.kafka.event.EventCreated;
import org.example.common.kafka.event.EventUpdated;
import org.example.common.kafka.event.UserCreatedEvent;
import org.example.common.kafka.event.UserDeletedEvent;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.entity.NotificationType;
import org.example.notificationservice.repository.NotificationRepository;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = KafkaTopics.USER_CREATED_TOPIC)
    public void handleCreatedUser(
            ConsumerRecord<String, UserCreatedEvent> record) {
        putToMdc(record);

        try {
            UserCreatedEvent event = record.value();
            Notification notification = Notification.builder()
                    .userId(event.getId())
                    .username(event.getUsername())
                    .type(NotificationType.USER_CREATED)
                    .message("Welcome to booking events service")
                    .createdAt(event.getOccurredAt())
                    .read(false)
                    .build();
            notificationRepository.save(notification);
        } finally {
            clear();
        }
    }

    @KafkaListener(topics = KafkaTopics.USER_DELETED_TOPIC)
    public void handleUserDeleted(
            ConsumerRecord<String, UserDeletedEvent> record) {
        putToMdc(record);
        try {
            UserDeletedEvent event = record.value();
            notificationRepository.deleteAllByUserId(event.getUserId());
        } finally {
            clear();
        }
    }

    @KafkaListener(topics = KafkaTopics.EVENT_CREATED_TOPIC)
    public void handleCreatedEvent(
            ConsumerRecord<String, EventCreated> record) {
        putToMdc(record);
        try {
            EventCreated event = record.value();
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .type(NotificationType.EVENT_CREATED)
                    .message(event.getTitle() + " event has been created," +
                            " event date - " + event.getEventDate() +
                            ". With event location - " + event.getLocation())
                    .createdAt(event.getOccurredAt())
                    .read(false)
                    .build();
            notificationRepository.save(notification);
        } finally {
            clear();
        }
    }

    @KafkaListener(topics = KafkaTopics.EVENT_UPDATED_TOPIC)
    public void handleUpdatedEvent(
            ConsumerRecord<String, EventUpdated> record) {
        putToMdc(record);
        try {
            EventUpdated event = record.value();
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .type(NotificationType.EVENT_UPDATED)
                    .message(event.getTitle() + " event has been updated," +
                            " new event date - " + event.getEventDate() +
                            ". With event new location - " + event.getLocation())
                    .createdAt(event.getOccurredAt())
                    .read(false)
                    .build();
            notificationRepository.save(notification);
        } finally {
            clear();
        }
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_APPROVED_TOPIC)
    public void handleApproved(
            ConsumerRecord<String, BookingCreatedEvent> record) {
        putToMdc(record);
        try {
            BookingCreatedEvent event = record.value();
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .type(NotificationType.BOOKING_APPROVED)
                    .message("Booking for event with id -" + event.getEventId() + " has been approved.")
                    .createdAt(event.getOccurredAt())
                    .read(false)
                    .build();
            notificationRepository.save(notification);
        } finally {
            clear();
        }
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CANCELLED_TOPIC)
    public void handleCancelled(
            ConsumerRecord<String, BookingCancelledEvent> record) {
        putToMdc(record);
        try {
            BookingCancelledEvent event = record.value();
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .type(NotificationType.BOOKING_CANCELLED)
                    .message("Booking for event with id -" + event.getEventId() + " has been cancelled.")
                    .createdAt(event.getOccurredAt())
                    .read(false)
                    .build();
            notificationRepository.save(notification);
        } finally {
            clear();
        }
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CREATED_TOPIC)
    public void handleBookingCreated(
            ConsumerRecord<String, BookingCreatedEvent> record) {
        putToMdc(record);
        try {
            BookingCreatedEvent event = record.value();
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .type(NotificationType.BOOKING_CREATED)
                    .message("Booking for event with id -" + event.getEventId() + " is pending now.")
                    .createdAt(event.getOccurredAt())
                    .read(false)
                    .build();
            notificationRepository.save(notification);
        } finally {
            clear();
        }
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_DELETED_TOPIC)
    public void handleBookingDeleted(
            ConsumerRecord<String, BookingDeletedEvent> record) {
        putToMdc(record);
        try {
            BookingDeletedEvent event = record.value();
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .type(NotificationType.BOOKING_DELETED)
                    .message("Booking for event with id -" + event.getEventId() + " successfully deleted.")
                    .createdAt(event.getOccurredAt())
                    .read(false)
                    .build();
            notificationRepository.save(notification);
        } finally {
            clear();
        }
    }

    private void putToMdc(ConsumerRecord<?, ?> record) {
        Header header = record.headers()
                .lastHeader(CorrelationConstants.CORRELATION_ID);

        if (header != null) {
            MDC.put(
                    CorrelationConstants.MDC_KEY,
                    new String(header.value(), StandardCharsets.UTF_8)
            );
        }
    }

    public static void clear() {
        MDC.remove(CorrelationConstants.MDC_KEY);
    }
}
