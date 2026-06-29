package org.example.bookingservice.service;

import org.example.bookingservice.dto.response.BookingResponse;
import org.example.bookingservice.dto.request.BookingRequest;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.EventDeleted;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface BookingService {
    Page<BookingResponse> findAllByUserId(Long userId, Pageable pageable);

    BookingResponse findById(Long id, Long userId);

    BookingResponse create(BookingRequest request, Long userId, String username);

    void delete(Long id, Long userId, String username);

    void approve(BookingCreatedEvent event);

    void setCancelledByEvent(EventDeleted event);
}
