package org.example.bookingservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.bookingservice.dto.BookingDto;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingEventProducer producer;

    @Override
    public List<BookingDto> findAllByUserId(Long userId) {
        return bookingRepository.findAllByUserId(userId).stream().map(bookingMapper::toDto).toList();
    }

    @Override
    public BookingDto findById(Long id, Long userId) {
        Booking booking = bookingRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        validateUser(booking, userId);
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto create(BookingRequest request, Long userId, String username) {
        Booking booking = bookingMapper.toEntity(request);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);
        producer.sendBookingCreated(BookingCreatedEvent.builder()
                .eventId(booking.getEventId())
                .username(username)
                .userId(booking.getUserId())
                .build());
        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId, String username) {
        Booking booking = bookingRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        validateUser(booking, userId);
        bookingRepository.delete(booking);
        producer.sendBookingDeleted(BookingDeletedEvent.builder()
                .eventId(id)
                .username(username)
                .build());
    }

    @Override
    @Transactional
    public void approve(BookingCreatedEvent event) {
        Booking booking = bookingRepository.findByUserIdAndEventId(event.getUserId(), event.getEventId()).orElseThrow(EntityNotFoundException::new);
        booking.setStatus(BookingStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void setCancelledByEvent(EventDeleted event) {
        bookingRepository.updateStatusByEventId(event.getEventId(), BookingStatus.CANCELLED);
        producer.sendBookingCancelled(BookingCancelledEvent.builder()
                .userId(event.getUserId())
                .eventId(event.getEventId())
                .username(event.getUsername())
                .build());
    }

    private void validateUser(Booking booking, Long userId) {
        if (!booking.getUserId().equals(userId)) {
            throw new AccessDeniedException(
                    "Not allowed to access this booking");
        }
    }
}
