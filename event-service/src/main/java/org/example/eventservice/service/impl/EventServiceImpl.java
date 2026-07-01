package org.example.eventservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.EventCreated;
import org.example.common.kafka.event.EventDeleted;
import org.example.common.kafka.event.EventUpdated;
import org.example.eventservice.dto.response.EventPageResponse;
import org.example.eventservice.dto.response.EventResponse;
import org.example.eventservice.dto.request.CreateEventRequest;
import org.example.eventservice.entity.Event;
import org.example.eventservice.kafka.producer.EventProducer;
import org.example.eventservice.mapper.EventMapper;
import org.example.eventservice.repository.EventRepository;
import org.example.eventservice.service.EventService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final EventProducer eventProducer;

    @Override
    @Cacheable(value = "events")
    public EventPageResponse getEvents(Pageable pageable) {
        log.info("Request to fetch all events");

        Page<Event> events = eventRepository.findAll(pageable);

        log.debug("Found {} events in total", events.getTotalElements());
        return toPageResponse(events.map(eventMapper::toDto));
    }

    @Override
    @Cacheable(value = "user-events", key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public EventPageResponse getUserEvents(Long userId, Pageable pageable) {
        log.info("Request to fetch events for user ID: {}", userId);

        Page<Event> userEvents = eventRepository.findByUserId(userId, pageable);

        log.debug("Found {} events for user ID: {}", userEvents.getTotalElements(), userId);

        return toPageResponse(userEvents.map(eventMapper::toDto));
    }

    @Override
    @Cacheable(value = "events", key = "#eventId")
    public EventResponse getEvent(Long eventId) {
        log.info("Fetching event with ID: {}", eventId);

        return eventRepository.findById(eventId)
                .map(eventMapper::toDto)
                .orElseThrow(() -> {
                    log.warn("Event fetching failed: Event not found with ID: {}", eventId);
                    return new EntityNotFoundException("Event not found with id: " + eventId);
                });
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "events", allEntries = true),
                    @CacheEvict(value = "user-events", allEntries = true)
            })
    public EventResponse create(CreateEventRequest dto, Long userId, String username) {
        log.info("Creating new event titled: '{}' by user: '{}' (ID: {})", dto.getTitle(), username, userId);

        Event event = eventMapper.toEntity(dto);
        event.setUserId(userId);
        Event saved = eventRepository.save(event);

        log.info("Event successfully saved with ID: {}", saved.getId());

        eventProducer.sendEventCreated(EventCreated.builder()
                .title(saved.getTitle())
                .userId(userId)
                .username(username)
                .eventDate(saved.getEventDate())
                .location(saved.getLocation())
                .id(saved.getId())
                .build());

        log.debug("EventCreated notification sent via producer for event ID: {}", saved.getId());

        return eventMapper.toDto(saved);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "events", allEntries = true),
                    @CacheEvict(value = "user-events", allEntries = true)
            })
    public EventResponse update(CreateEventRequest dto, Long userId, Long id, String username) {
        log.info("Attempting to update event ID: {} by user: '{}' (ID: {})", id, username, userId);

        Event oldEvent = eventRepository.findById(id).orElseThrow(() -> {
            log.warn("Update failed: Event not found with ID: {}", id);
            return new EntityNotFoundException("Event not found");
        });

        validateUser(oldEvent, userId);
        oldEvent.setCapacity(dto.getCapacity());
        oldEvent.setEventDate(dto.getEventDate());
        oldEvent.setDescription(dto.getDescription());
        oldEvent.setLocation(dto.getLocation());
        oldEvent.setTitle(dto.getTitle());

        log.info("Event ID: {} fields updated in transaction", id);

        eventProducer.sendEventUpdated(EventUpdated.builder()
                .userId(userId)
                .title(oldEvent.getTitle())
                .username(username)
                .eventDate(oldEvent.getEventDate())
                .location(oldEvent.getLocation())
                .id(id)
                .build());

        log.debug("EventUpdated notification sent via producer for event ID: {}", id);

        return eventMapper.toDto(oldEvent);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "events", allEntries = true),
                    @CacheEvict(value = "user-events", allEntries = true)
            }
    )
    public void delete(Long id, Long userId, String username) {
        log.info("Attempting to delete event ID: {} by user: '{}' (ID: {})", id, username, userId);

        Event event = eventRepository.findById(id).orElseThrow(() -> {
            log.warn("Deletion failed: Event not found with ID: {}", id);
            return new EntityNotFoundException("Event not found");
        });

        validateUser(event, userId);
        eventRepository.delete(event);
        log.info("Event ID: {} successfully removed from repository", id);

        eventProducer.sendEventDeleted(EventDeleted.builder()
                .title(event.getTitle())
                .username(username)
                .userId(userId)
                .eventId(event.getId())
                .build());

        log.debug("EventDeleted notification sent via producer for event ID: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public void reserveSeat(BookingCreatedEvent event) {
        log.info("Processing seat reservation for event ID: {}", event.getEventId());

        if (eventRepository.reserveSeat(event.getEventId()) == 0) {
            log.warn("Reservation failed: No available seats left for event ID: {}", event.getEventId());
            throw new IllegalStateException("No available seats");
        }

        log.info("Seat successfully reserved for event ID: {}", event.getEventId());
        eventProducer.sendBookingApproved(event);
        log.debug("BookingApproved confirmation sent for event ID: {}", event.getEventId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public void cancelReservation(Long eventId) {
        log.info("Processing reservation cancellation for event ID: {}", eventId);
        if (eventRepository.cancelReservation(eventId) == 0) {
            log.warn("Cancellation failed: Available seats already equal total capacity for event ID: {}", eventId);
            throw new IllegalStateException("Available seats already equal capacity");
        }
        log.info("Reservation successfully cancelled for event ID: {}", eventId);
    }

    private void validateUser(Event event, Long userId) {
        if (!event.getUserId().equals(userId)) {
            log.error("Access denied: User ID {} is not authorized to modify event ID {} (Owner ID: {})",
                    userId, event.getId(), event.getUserId());
            throw new AccessDeniedException(
                    "Not allowed to modify this event");
        }
    }

    private EventPageResponse toPageResponse(Page<EventResponse> page) {
        return EventPageResponse.builder()
                .content(page.getContent())
                .number(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
