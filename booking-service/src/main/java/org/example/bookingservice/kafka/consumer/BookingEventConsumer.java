package org.example.bookingservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.service.BookingService;
import org.example.common.correlation.CorrelationConstants;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.EventDeleted;
import org.example.common.kafka.event.UserDeletedEvent;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class BookingEventConsumer {
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @KafkaListener(topics = KafkaTopics.USER_DELETED_TOPIC)
    public void handleUserDeleted(
            ConsumerRecord<String, UserDeletedEvent> record) {
        putToMdc(record);
        try {
            UserDeletedEvent event = record.value();
            bookingRepository.deleteAllByUserId(event.getUserId());
        } finally {
            clear();
        }
    }

    @KafkaListener(topics = KafkaTopics.EVENT_DELETED_TOPIC)
    public void handleEventDeleted(
            ConsumerRecord<String, EventDeleted> record) {
        putToMdc(record);
        try {
            EventDeleted event = record.value();
            bookingService.setCancelledByEvent(event);
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
            bookingService.approve(event);
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
