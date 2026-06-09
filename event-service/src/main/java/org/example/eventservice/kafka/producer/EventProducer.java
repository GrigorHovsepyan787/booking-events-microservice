package org.example.eventservice.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.EventCreated;
import org.example.common.kafka.event.EventDeleted;
import org.example.common.kafka.event.EventUpdated;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendEventCreated(EventCreated event) {
        send(KafkaTopics.EVENT_CREATED_TOPIC, event);
    }

    public void sendEventUpdated(EventUpdated event) {
        send(KafkaTopics.EVENT_UPDATED_TOPIC, event);
    }

    public void sendEventDeleted(EventDeleted event){
        send(KafkaTopics.EVENT_DELETED_TOPIC, event);
    }

    public void sendBookingApproved(BookingCreatedEvent event) {
        send(KafkaTopics.BOOKING_APPROVED_TOPIC, event);
    }

    private void send(String topic, Object event) {
        kafkaTemplate.send(topic, event).whenComplete((result, ex) -> {
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
