package org.example.common.kafka.event;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public abstract class BaseEvent {
    private final int version = 1;
    private final LocalDateTime occurredAt = LocalDateTime.now();
}
