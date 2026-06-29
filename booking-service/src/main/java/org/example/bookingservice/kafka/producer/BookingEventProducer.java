package org.example.bookingservice.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.common.correlation.CorrelationConstants;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.BookingCancelledEvent;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.BookingDeletedEvent;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendBookingCreated(BookingCreatedEvent event) {
        send(KafkaTopics.BOOKING_CREATED_TOPIC, event.getUserId().toString(), event);
    }

    public void sendBookingDeleted(BookingDeletedEvent event) {
        send(KafkaTopics.BOOKING_DELETED_TOPIC, event.getUserId().toString(), event);
    }

    public void sendBookingCancelled(BookingCancelledEvent event) {
        send(KafkaTopics.BOOKING_CANCELLED_TOPIC, event.getUserId().toString(), event);
    }

    private void send(String topic, String messageKey, Object event) {
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topic, messageKey, event);
        String correlationId = MDC.get(CorrelationConstants.MDC_KEY);

        if (correlationId != null) {
            record.headers().add(
                    CorrelationConstants.CORRELATION_ID,
                    correlationId.getBytes(StandardCharsets.UTF_8)
            );
        }
        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Message sent successfully to topic: {}, partition: {}, offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Message sending has failed. Event data:{}", event, ex);
            }
        });
    }
}
