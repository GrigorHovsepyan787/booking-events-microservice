package org.example.userservice.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.common.correlation.CorrelationConstants;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.UserCreatedEvent;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserCreated(UserCreatedEvent event) {
        send(KafkaTopics.USER_CREATED_TOPIC, event.getId().toString(), event);
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
