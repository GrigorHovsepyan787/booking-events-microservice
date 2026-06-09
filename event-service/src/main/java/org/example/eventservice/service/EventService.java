package org.example.eventservice.service;

import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.eventservice.dto.CreateEventDto;
import org.example.eventservice.dto.EventDto;

import java.util.List;

public interface EventService {
    List<EventDto> getEvents();
    List<EventDto> getUserEvents(Long userId);
    EventDto getEvent(Long eventId);
    EventDto create(CreateEventDto dto, Long userId, String username);
    EventDto update(CreateEventDto dto, Long userId, Long id, String username);
    void delete(Long id, Long userId, String username);
    void reserveSeat(BookingCreatedEvent event);
    void cancelReservation(Long eventId);
}
