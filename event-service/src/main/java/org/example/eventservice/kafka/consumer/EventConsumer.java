package org.example.eventservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.example.common.correlation.CorrelationConstants;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.BookingDeletedEvent;
import org.example.common.kafka.event.UserDeletedEvent;
import org.example.eventservice.repository.EventRepository;
import org.example.eventservice.service.EventService;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class EventConsumer {
    private final EventService eventService;
    private final EventRepository eventRepository;

    @KafkaListener(topics = KafkaTopics.BOOKING_CREATED_TOPIC)
    public void handleBookingCreated(
            ConsumerRecord<String, BookingCreatedEvent> record) {
        putToMdc(record);
        try {
            BookingCreatedEvent event = record.value();
            eventService.reserveSeat(event);
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
            if (event != null && event.getEventId() != null) {
                eventService.cancelReservation(event.getEventId());
            }
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
            if (event != null && event.getUserId() != null) {
                eventRepository.deleteAllByUserId(event.getUserId());
            }
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
