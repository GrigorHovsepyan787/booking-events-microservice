package org.example.eventservice.service;

import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.eventservice.dto.response.EventPageResponse;
import org.example.eventservice.dto.response.EventResponse;
import org.example.eventservice.dto.request.CreateEventRequest;
import org.springframework.data.domain.Pageable;

public interface EventService {
    EventPageResponse getEvents(Pageable pageable);

    EventPageResponse getUserEvents(Long userId, Pageable pageable);

    EventResponse getEvent(Long eventId);

    EventResponse create(CreateEventRequest dto, Long userId, String username);

    EventResponse update(CreateEventRequest dto, Long userId, Long id, String username);

    void delete(Long id, Long userId, String username);

    void reserveSeat(BookingCreatedEvent event);

    void cancelReservation(Long eventId);
}
