package org.example.bookingservice.service;

import org.example.bookingservice.dto.BookingDto;
import org.example.bookingservice.dto.request.BookingRequest;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.EventDeleted;

import java.util.List;

public interface BookingService {
    List<BookingDto> findAllByUserId(Long userId);

    BookingDto findById(Long id, Long userId);

    BookingDto create(BookingRequest request, Long userId, String username);

    void delete(Long id, Long userId, String username);

    void approve(BookingCreatedEvent event);

    void setCancelledByEvent(EventDeleted event);
}
