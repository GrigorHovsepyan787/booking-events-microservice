package org.example.userservice.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.kafka.KafkaTopics;
import org.example.common.kafka.event.UserCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserCreated(UserCreatedEvent event) {
        send(KafkaTopics.USER_CREATED_TOPIC, event);
    }


    private void send(String topic, Object event){
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
