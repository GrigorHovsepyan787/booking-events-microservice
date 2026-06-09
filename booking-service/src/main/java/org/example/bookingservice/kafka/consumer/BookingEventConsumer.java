package org.example.bookingservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.service.BookingService;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.EventDeleted;
import org.example.common.kafka.event.UserDeletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventConsumer {
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @KafkaListener(topics = KafkaTopics.USER_DELETED_TOPIC)
    public void handleUserDeleted(UserDeletedEvent event) {
        bookingRepository.deleteAllByUserId(event.getUserId());
    }

    @KafkaListener(topics = KafkaTopics.EVENT_DELETED_TOPIC)
    public void handleEventDeleted(EventDeleted event) {
        bookingService.setCancelledByEvent(event);
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_APPROVED_TOPIC)
    public void handleApproved(BookingCreatedEvent event) {
        bookingService.approve(event);
    }
}
