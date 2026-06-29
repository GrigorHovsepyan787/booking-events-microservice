package org.example.common.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class EventCreated extends BaseEvent{
    private Long id;
    private Long userId;
    private String username;
    private String title;
    private LocalDateTime eventDate;
    private String location;
}
