package org.example.bookingservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bookingservice.dto.response.BookingResponse;
import org.example.bookingservice.dto.request.BookingRequest;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.BookingStatus;
import org.example.bookingservice.kafka.producer.BookingEventProducer;
import org.example.bookingservice.mapper.BookingMapper;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.service.BookingService;
import org.example.common.kafka.event.BookingCancelledEvent;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.BookingDeletedEvent;
import org.example.common.kafka.event.EventDeleted;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingEventProducer producer;

    @Override
    public Page<BookingResponse> findAllByUserId(Long userId, Pageable pageable) {
        log.info("Fetching all bookings for user ID: {}", userId);
        Page<Booking> bookings = bookingRepository.findAllByUserId(userId, pageable);
        log.debug("Found {} bookings for user ID: {}", bookings.getTotalElements(), userId);
        return bookings.map(bookingMapper::toDto);
    }

    @Override
    public BookingResponse findById(Long id, Long userId) {
        log.info("Fetching booking ID: {} for user ID: {}", id, userId);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Booking fetching failed: Booking not found with ID: {}", id);
                    return new EntityNotFoundException("Booking not found");
                });

        validateUser(booking, userId);
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingResponse create(BookingRequest request, Long userId, String username) {
        log.info("Creating booking for event ID: {} by user: '{}' (ID: {})", request.getEventId(), username, userId);

        Booking booking = bookingMapper.toEntity(request);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);

        log.info("Booking successfully saved with ID: {} and status: {}", booking.getId(), booking.getStatus());

        producer.sendBookingCreated(BookingCreatedEvent.builder()
                .eventId(booking.getEventId())
                .username(username)
                .userId(booking.getUserId())
                .build());

        log.debug("BookingCreatedEvent dispatched for event ID: {}", booking.getEventId());

        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId, String username) {
        log.info("Attempting to delete booking ID: {} by user: '{}' (ID: {})", id, username, userId);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Deletion failed: Booking not found with ID: {}", id);
                    return new EntityNotFoundException("Booking not found");
                });

        validateUser(booking, userId);
        bookingRepository.delete(booking);

        log.info("Booking ID: {} successfully deleted", id);

        producer.sendBookingDeleted(BookingDeletedEvent.builder()
                .eventId(id)
                .username(username)
                .build());
        log.debug("BookingDeletedEvent dispatched for booking ID: {}", id);
    }

    @Override
    @Transactional
    public void approve(BookingCreatedEvent event) {
        log.info("Processing asynchronous booking approval for user ID: {} and event ID: {}", event.getUserId(), event.getEventId());
        Booking booking = bookingRepository.findByUserIdAndEventId(event.getUserId(), event.getEventId())
                .orElseThrow(() -> {
                    log.error("Approval failed: Booking not found for user ID: {} and event ID: {}", event.getUserId(), event.getEventId());
                    return new EntityNotFoundException("Booking not found");
                });

        booking.setStatus(BookingStatus.CONFIRMED);
        log.info("Booking ID: {} status updated to CONFIRMED", booking.getId());
    }

    @Override
    @Transactional
    public void setCancelledByEvent(EventDeleted event) {
        log.info("Processing booking cancellations due to event deletion (Event ID: {})", event.getEventId());

        bookingRepository.updateStatusByEventId(event.getEventId(), BookingStatus.CANCELLED);

        log.info("All bookings for event ID: {} have been set to CANCELLED", event.getEventId());

        producer.sendBookingCancelled(BookingCancelledEvent.builder()
                .userId(event.getUserId())
                .eventId(event.getEventId())
                .username(event.getUsername())
                .build());

        log.debug("BookingCancelledEvent dispatched for event ID: {}", event.getEventId());
    }

    private void validateUser(Booking booking, Long userId) {
        if (!booking.getUserId().equals(userId)) {
            log.error("Access denied: User ID {} is not allowed to access booking ID {} owned by User ID {}",
                    userId, booking.getId(), booking.getUserId());
            throw new AccessDeniedException(
                    "Not allowed to access this booking");
        }
    }
}
