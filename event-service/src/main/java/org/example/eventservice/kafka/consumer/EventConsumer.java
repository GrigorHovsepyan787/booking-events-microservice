package org.example.eventservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.BookingDeletedEvent;
import org.example.common.kafka.event.UserDeletedEvent;
import org.example.eventservice.repository.EventRepository;
import org.example.eventservice.service.EventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventConsumer {
    private final EventService eventService;
    private final EventRepository eventRepository;

    @KafkaListener(topics = KafkaTopics.BOOKING_CREATED_TOPIC)
    public void handleBookingCreated(BookingCreatedEvent event) {
        eventService.reserveSeat(event);
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_DELETED_TOPIC)
    public void handleBookingDeleted(BookingDeletedEvent event) {
        eventService.cancelReservation(event.getEventId());
    }

    @KafkaListener(topics = KafkaTopics.USER_DELETED_TOPIC)
    public void handleUserDeleted(UserDeletedEvent event){
        eventRepository.deleteAllByUserId(event.getUserId());
    }
}
