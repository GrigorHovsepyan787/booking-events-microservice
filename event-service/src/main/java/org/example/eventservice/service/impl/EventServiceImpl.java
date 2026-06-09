package org.example.eventservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.EventCreated;
import org.example.common.kafka.event.EventDeleted;
import org.example.common.kafka.event.EventUpdated;
import org.example.eventservice.dto.CreateEventDto;
import org.example.eventservice.dto.EventDto;
import org.example.eventservice.entity.Event;
import org.example.eventservice.kafka.producer.EventProducer;
import org.example.eventservice.mapper.EventMapper;
import org.example.eventservice.repository.EventRepository;
import org.example.eventservice.service.EventService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final EventProducer eventProducer;

    @Override
    public List<EventDto> getEvents() {
        return eventRepository.findAll().stream().map(eventMapper::toDto).toList();
    }

    @Override
    public List<EventDto> getUserEvents(Long userId) {
        return eventRepository.findByUserId(userId).stream().map(eventMapper::toDto).toList();
    }

    @Override
    public EventDto getEvent(Long eventId) {
        return eventMapper.toDto(eventRepository.findById(eventId).orElseThrow(EntityNotFoundException::new));
    }

    @Override
    @Transactional
    public EventDto create(CreateEventDto dto, Long userId, String username) {
        Event event = eventMapper.toEntity(dto);
        event.setUserId(userId);
        Event saved = eventRepository.save(event);
        eventProducer.sendEventCreated(EventCreated.builder()
                .title(saved.getTitle())
                .username(username)
                .eventDate(saved.getEventDate())
                .location(saved.getLocation())
                .id(saved.getId())
                .build());
        return eventMapper.toDto(saved);
    }

    @Override
    @Transactional
    public EventDto update(CreateEventDto dto, Long userId, Long id, String username) {
        Event oldEvent = eventRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        validateUser(oldEvent, userId);
        oldEvent.setCapacity(dto.getCapacity());
        oldEvent.setEventDate(dto.getEventDate());
        oldEvent.setDescription(dto.getDescription());
        oldEvent.setLocation(dto.getLocation());
        oldEvent.setTitle(dto.getTitle());
        eventProducer.sendEventUpdated(EventUpdated.builder()
                .userId(userId)
                .title(oldEvent.getTitle())
                .username(username)
                .eventDate(oldEvent.getEventDate())
                .location(oldEvent.getLocation())
                .id(id)
                .build());
        return eventMapper.toDto(oldEvent);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId, String username) {
        Event event = eventRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        validateUser(event, userId);
        eventRepository.delete(event);
        eventProducer.sendEventDeleted(EventDeleted.builder()
                .title(event.getTitle())
                .username(username)
                .userId(userId)
                .eventId(event.getId())
                .build());
    }

    @Override
    @Transactional
    public void reserveSeat(BookingCreatedEvent event) {
        if (eventRepository.reserveSeat(event.getEventId()) == 0) {
            throw new IllegalStateException("No available seats");
        }
        eventProducer.sendBookingApproved(event);
    }

    @Override
    @Transactional
    public void cancelReservation(Long eventId) {
        if (eventRepository.cancelReservation(eventId) == 0) {
            throw new IllegalStateException("Available seats already equal capacity");
        }
    }

    private void validateUser(Event event, Long userId) {
        if (!event.getUserId().equals(userId)) {
            throw new AccessDeniedException(
                    "Not allowed to modify this event");
        }
    }
}
