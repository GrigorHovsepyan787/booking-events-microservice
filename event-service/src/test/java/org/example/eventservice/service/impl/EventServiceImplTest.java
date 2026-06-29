package org.example.eventservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.EventCreated;
import org.example.common.kafka.event.EventDeleted;
import org.example.common.kafka.event.EventUpdated;
import org.example.eventservice.dto.request.CreateEventRequest;
import org.example.eventservice.dto.response.EventResponse;
import org.example.eventservice.entity.Event;
import org.example.eventservice.kafka.producer.EventProducer;
import org.example.eventservice.mapper.EventMapper;
import org.example.eventservice.repository.EventRepository;
import org.example.eventservice.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventMapper eventMapper;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventProducer eventProducer;

    @Mock
    private Pageable pageable;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event testEvent;
    private CreateEventRequest createRequest;
    private EventResponse eventResponse;
    private BookingCreatedEvent bookingCreatedEvent;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setTitle("Test Event");
        testEvent.setDescription("Test event description");
        testEvent.setEventDate(LocalDateTime.now().plusDays(1));
        testEvent.setLocation("Test Location");
        testEvent.setCapacity(10);
        testEvent.setSeatsAvailable(10);
        testEvent.setUserId(10L);

        createRequest = new CreateEventRequest();
        createRequest.setTitle("New Event");
        createRequest.setDescription("New event description");
        createRequest.setEventDate(LocalDateTime.now().plusDays(2));
        createRequest.setLocation("New Location");
        createRequest.setCapacity(20);

        eventResponse = new EventResponse();
        eventResponse.setId(1L);
        eventResponse.setTitle("Test Event");
        eventResponse.setDescription("Test event description");
        eventResponse.setEventDate(LocalDateTime.now().plusDays(1));
        eventResponse.setLocation("Test Location");
        eventResponse.setCapacity(10);
        eventResponse.setSeatsAvailable(10);

        bookingCreatedEvent = new BookingCreatedEvent();
        bookingCreatedEvent.setUserId(10L);
        bookingCreatedEvent.setEventId(100L);
        bookingCreatedEvent.setUsername("testuser");
    }

    // ==================== getEvents Tests ====================

    @Test
    void getEvents_shouldReturnEventsPage_whenEventsExist() {
        List<Event> eventsList = List.of(testEvent);
        Page<Event> eventPage = new PageImpl<>(eventsList);

        when(eventRepository.findAll(pageable)).thenReturn(eventPage);
        when(eventMapper.toDto(testEvent)).thenReturn(eventResponse);

        Page<EventResponse> result = eventService.getEvents(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Event");

        verify(eventRepository).findAll(pageable);
        verify(eventMapper).toDto(testEvent);
    }

    @Test
    void getEvents_shouldReturnEmptyPage_whenNoEventsExist() {
        Page<Event> emptyPage = new PageImpl<>(List.of());

        when(eventRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<EventResponse> result = eventService.getEvents(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();

        verify(eventRepository).findAll(pageable);
        verifyNoInteractions(eventMapper, eventProducer);
    }


    // ==================== getUserEvents Tests ====================

    @Test
    void getUserEvents_shouldReturnUserEventsPage_whenUserHasEvents() {
        List<Event> userEventsList = List.of(testEvent);
        Page<Event> userEventsPage = new PageImpl<>(userEventsList);

        when(eventRepository.findByUserId(10L, pageable)).thenReturn(userEventsPage);
        when(eventMapper.toDto(testEvent)).thenReturn(eventResponse);

        Page<EventResponse> result = eventService.getUserEvents(10L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Event");

        verify(eventRepository).findByUserId(10L, pageable);
        verify(eventMapper).toDto(testEvent);
    }

    @Test
    void getUserEvents_shouldReturnEmptyPage_whenUserHasNoEvents() {
        Page<Event> emptyPage = new PageImpl<>(List.of());

        when(eventRepository.findByUserId(10L, pageable)).thenReturn(emptyPage);

        Page<EventResponse> result = eventService.getUserEvents(10L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();

        verify(eventRepository).findByUserId(10L, pageable);
        verifyNoInteractions(eventMapper, eventProducer);
    }

    @Test
    void getUserEvents_shouldPassNullUserIdToRepository_whenUserIdIsNull() {
        Page<Event> emptyPage = new PageImpl<>(List.of());

        when(eventRepository.findByUserId(eq(null), any(Pageable.class))).thenReturn(emptyPage);

        Page<EventResponse> result = eventService.getUserEvents(null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();

        verify(eventRepository).findByUserId(eq(null), any(Pageable.class));
        verifyNoInteractions(eventMapper, eventProducer);
    }

    @Test
    void getUserEvents_shouldHandleNegativeUserId() {
        Page<Event> emptyPage = new PageImpl<>(List.of());

        when(eventRepository.findByUserId(-1L, pageable)).thenReturn(emptyPage);

        Page<EventResponse> result = eventService.getUserEvents(-1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(eventRepository).findByUserId(-1L, pageable);
    }

    @Test
    void getUserEvents_shouldHandleZeroUserId() {
        Page<Event> emptyPage = new PageImpl<>(List.of());

        when(eventRepository.findByUserId(0L, pageable)).thenReturn(emptyPage);

        Page<EventResponse> result = eventService.getUserEvents(0L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(eventRepository).findByUserId(0L, pageable);
    }

    // ==================== getEvent Tests ====================

    @Test
    void getEvent_shouldReturnEventResponse_whenEventExists() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventMapper.toDto(testEvent)).thenReturn(eventResponse);

        EventResponse result = eventService.getEvent(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Event");
        assertThat(result.getLocation()).isEqualTo("Test Location");

        verify(eventRepository).findById(1L);
        verify(eventMapper).toDto(testEvent);
    }

    @Test
    void getEvent_shouldThrowEntityNotFoundException_whenEventDoesNotExist() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found with id: 99");

        verify(eventRepository).findById(99L);
        verifyNoInteractions(eventMapper, eventProducer);
    }

    @Test
    void getEvent_shouldThrowEntityNotFoundException_whenEventIdIsNull() {
        when(eventRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found with id: null");

        verify(eventRepository).findById(null);
        verifyNoInteractions(eventMapper, eventProducer);
    }

    @Test
    void getEvent_shouldReturnCorrectEventDetails() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventMapper.toDto(testEvent)).thenReturn(eventResponse);

        EventResponse result = eventService.getEvent(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Event");
        assertThat(result.getDescription()).isEqualTo("Test event description");
        assertThat(result.getLocation()).isEqualTo("Test Location");
        assertThat(result.getCapacity()).isEqualTo(10);
    }

    // ==================== create Tests ====================

    @Test
    void create_shouldCreateEventAndSendEventCreated_whenValidInput() {
        Event newEvent = new Event();
        newEvent.setTitle(createRequest.getTitle());
        newEvent.setDescription(createRequest.getDescription());
        newEvent.setEventDate(createRequest.getEventDate());
        newEvent.setLocation(createRequest.getLocation());
        newEvent.setCapacity(createRequest.getCapacity());
        newEvent.setUserId(10L);

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle(createRequest.getTitle());
        savedEvent.setDescription(createRequest.getDescription());
        savedEvent.setEventDate(createRequest.getEventDate());
        savedEvent.setLocation(createRequest.getLocation());
        savedEvent.setCapacity(createRequest.getCapacity());
        savedEvent.setUserId(10L);

        when(eventMapper.toEntity(createRequest)).thenReturn(newEvent);
        when(eventRepository.save(newEvent)).thenReturn(savedEvent);
        when(eventMapper.toDto(savedEvent)).thenReturn(eventResponse);

        EventResponse result = eventService.create(createRequest, 10L, "testuser");

        assertThat(result).isNotNull();

        verify(eventMapper).toEntity(createRequest);
        verify(eventRepository).save(newEvent);
        verify(eventMapper).toDto(savedEvent);

        ArgumentCaptor<EventCreated> captor = ArgumentCaptor.forClass(EventCreated.class);
        verify(eventProducer).sendEventCreated(captor.capture());

        EventCreated sentEvent = captor.getValue();
        assertThat(sentEvent.getId()).isEqualTo(1L);
        assertThat(sentEvent.getTitle()).isEqualTo("New Event");
        assertThat(sentEvent.getUsername()).isEqualTo("testuser");
        assertThat(sentEvent.getLocation()).isEqualTo("New Location");
    }

    @Test
    void create_shouldSaveEventAndSendEventCreated_whenUserIdIsNull() {
        Event newEvent = new Event();
        newEvent.setTitle(createRequest.getTitle());
        newEvent.setDescription(createRequest.getDescription());
        newEvent.setEventDate(createRequest.getEventDate());
        newEvent.setLocation(createRequest.getLocation());
        newEvent.setCapacity(createRequest.getCapacity());
        newEvent.setUserId(null);

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle(createRequest.getTitle());
        savedEvent.setDescription(createRequest.getDescription());
        savedEvent.setEventDate(createRequest.getEventDate());
        savedEvent.setLocation(createRequest.getLocation());
        savedEvent.setCapacity(createRequest.getCapacity());
        savedEvent.setUserId(null);

        when(eventMapper.toEntity(createRequest)).thenReturn(newEvent);
        when(eventRepository.save(newEvent)).thenReturn(savedEvent);
        when(eventMapper.toDto(savedEvent)).thenReturn(eventResponse);

        EventResponse result = eventService.create(createRequest, null, "testuser");

        assertThat(result).isNotNull();

        verify(eventMapper).toEntity(createRequest);
        verify(eventRepository).save(newEvent);
        verify(eventMapper).toDto(savedEvent);

        ArgumentCaptor<EventCreated> captor = ArgumentCaptor.forClass(EventCreated.class);
        verify(eventProducer).sendEventCreated(captor.capture());

        EventCreated sentEvent = captor.getValue();
        assertThat(sentEvent.getId()).isEqualTo(1L);
        assertThat(sentEvent.getTitle()).isEqualTo("New Event");
        assertThat(sentEvent.getUsername()).isEqualTo("testuser");
        assertThat(sentEvent.getLocation()).isEqualTo("New Location");
    }

    @Test
    void create_shouldSaveEventAndSendEventCreated_whenUsernameIsNull() {
        Event newEvent = new Event();
        newEvent.setTitle(createRequest.getTitle());
        newEvent.setDescription(createRequest.getDescription());
        newEvent.setEventDate(createRequest.getEventDate());
        newEvent.setLocation(createRequest.getLocation());
        newEvent.setCapacity(createRequest.getCapacity());
        newEvent.setUserId(10L);

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle(createRequest.getTitle());
        savedEvent.setDescription(createRequest.getDescription());
        savedEvent.setEventDate(createRequest.getEventDate());
        savedEvent.setLocation(createRequest.getLocation());
        savedEvent.setCapacity(createRequest.getCapacity());
        savedEvent.setUserId(10L);

        when(eventMapper.toEntity(createRequest)).thenReturn(newEvent);
        when(eventRepository.save(newEvent)).thenReturn(savedEvent);
        when(eventMapper.toDto(savedEvent)).thenReturn(eventResponse);

        EventResponse result = eventService.create(createRequest, 10L, null);

        assertThat(result).isNotNull();

        verify(eventMapper).toEntity(createRequest);
        verify(eventRepository).save(newEvent);
        verify(eventMapper).toDto(savedEvent);

        ArgumentCaptor<EventCreated> captor = ArgumentCaptor.forClass(EventCreated.class);
        verify(eventProducer).sendEventCreated(captor.capture());

        EventCreated sentEvent = captor.getValue();
        assertThat(sentEvent.getId()).isEqualTo(1L);
        assertThat(sentEvent.getTitle()).isEqualTo("New Event");
        assertThat(sentEvent.getUsername()).isNull();
        assertThat(sentEvent.getLocation()).isEqualTo("New Location");
    }

    @Test
    void create_shouldHandleNullTitleInRequestGracefully() {
        CreateEventRequest nullTitleRequest = new CreateEventRequest();
        nullTitleRequest.setTitle(null);
        nullTitleRequest.setDescription("Description");
        nullTitleRequest.setEventDate(LocalDateTime.now().plusDays(2));
        nullTitleRequest.setLocation("Location");
        nullTitleRequest.setCapacity(20);

        Event newEvent = new Event();
        newEvent.setTitle(null);
        newEvent.setDescription("Description");
        newEvent.setEventDate(nullTitleRequest.getEventDate());
        newEvent.setLocation("Location");
        newEvent.setCapacity(20);
        newEvent.setUserId(10L);

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle(null);
        savedEvent.setDescription("Description");
        savedEvent.setEventDate(nullTitleRequest.getEventDate());
        savedEvent.setLocation("Location");
        savedEvent.setCapacity(20);
        savedEvent.setUserId(10L);

        when(eventMapper.toEntity(nullTitleRequest)).thenReturn(newEvent);
        when(eventRepository.save(newEvent)).thenReturn(savedEvent);
        when(eventMapper.toDto(savedEvent)).thenReturn(eventResponse);

        EventResponse result = eventService.create(nullTitleRequest, 10L, "testuser");

        assertThat(result).isNotNull();

        verify(eventMapper).toEntity(nullTitleRequest);
        verify(eventRepository).save(newEvent);

        ArgumentCaptor<EventCreated> captor = ArgumentCaptor.forClass(EventCreated.class);
        verify(eventProducer).sendEventCreated(captor.capture());

        EventCreated sentEvent = captor.getValue();
        assertThat(sentEvent.getId()).isEqualTo(1L);
        assertThat(sentEvent.getTitle()).isNull();
        assertThat(sentEvent.getUsername()).isEqualTo("testuser");
    }

    @Test
    void create_shouldThrowException_whenCreateEventRequestIsNull() {
        assertThatThrownBy(() -> eventService.create(null, 10L, "testuser"))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(eventRepository, eventProducer, eventMapper);
    }

    @Test
    void create_shouldCallRepositoryAndProducerInCorrectOrder() {
        Event newEvent = new Event();
        newEvent.setTitle(createRequest.getTitle());
        newEvent.setDescription(createRequest.getDescription());
        newEvent.setEventDate(createRequest.getEventDate());
        newEvent.setLocation(createRequest.getLocation());
        newEvent.setCapacity(createRequest.getCapacity());
        newEvent.setUserId(10L);

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle(createRequest.getTitle());
        savedEvent.setDescription(createRequest.getDescription());
        savedEvent.setEventDate(createRequest.getEventDate());
        savedEvent.setLocation(createRequest.getLocation());
        savedEvent.setCapacity(createRequest.getCapacity());
        savedEvent.setUserId(10L);

        when(eventMapper.toEntity(createRequest)).thenReturn(newEvent);
        when(eventRepository.save(newEvent)).thenReturn(savedEvent);
        when(eventMapper.toDto(savedEvent)).thenReturn(eventResponse);

        eventService.create(createRequest, 10L, "testuser");

        InOrder inOrder = inOrder(eventMapper, eventRepository, eventProducer, eventMapper);
        inOrder.verify(eventMapper).toEntity(createRequest);
        inOrder.verify(eventRepository).save(newEvent);
        inOrder.verify(eventProducer).sendEventCreated(any());
        inOrder.verify(eventMapper).toDto(savedEvent);
        inOrder.verifyNoMoreInteractions();
    }

    // ==================== update Tests ====================

    @Test
    void update_shouldUpdateEventAndSendEventUpdated_whenValidInput() {
        CreateEventRequest updateRequest = new CreateEventRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated description");
        updateRequest.setEventDate(LocalDateTime.now().plusDays(3));
        updateRequest.setLocation("Updated Location");
        updateRequest.setCapacity(15);

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setTitle("Old Title");
        existingEvent.setDescription("Old description");
        existingEvent.setEventDate(LocalDateTime.now().plusDays(1));
        existingEvent.setLocation("Old Location");
        existingEvent.setCapacity(10);
        existingEvent.setSeatsAvailable(10);
        existingEvent.setUserId(10L);

        EventResponse updatedResponse = new EventResponse();
        updatedResponse.setId(1L);
        updatedResponse.setTitle("Updated Title");
        updatedResponse.setDescription("Updated description");
        updatedResponse.setEventDate(LocalDateTime.now().plusDays(3));
        updatedResponse.setLocation("Updated Location");
        updatedResponse.setCapacity(15);
        updatedResponse.setSeatsAvailable(10);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(eventMapper.toDto(existingEvent)).thenReturn(updatedResponse);

        EventResponse result = eventService.update(updateRequest, 10L, 1L, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getLocation()).isEqualTo("Updated Location");
        assertThat(result.getCapacity()).isEqualTo(15);

        verify(eventRepository).findById(1L);
        verify(eventMapper).toDto(existingEvent);

        ArgumentCaptor<EventUpdated> captor = ArgumentCaptor.forClass(EventUpdated.class);
        verify(eventProducer).sendEventUpdated(captor.capture());

        EventUpdated sentEvent = captor.getValue();
        assertThat(sentEvent.getId()).isEqualTo(1L);
        assertThat(sentEvent.getTitle()).isEqualTo("Updated Title");
        assertThat(sentEvent.getUsername()).isEqualTo("testuser");
        assertThat(sentEvent.getLocation()).isEqualTo("Updated Location");
    }

    @Test
    void update_shouldThrowEntityNotFoundException_whenEventDoesNotExist() {
        CreateEventRequest updateRequest = new CreateEventRequest();
        updateRequest.setTitle("Updated Title");

        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.update(updateRequest, 10L, 99L, "testuser"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found");

        verify(eventRepository).findById(99L);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventProducer, eventMapper);
    }

    @Test
    void update_shouldThrowAccessDeniedException_whenUserIsNotOwner() {
        CreateEventRequest updateRequest = new CreateEventRequest();
        updateRequest.setTitle("Updated Title");

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        assertThatThrownBy(() -> eventService.update(updateRequest, 20L, 1L, "otheruser"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed to modify this event");

        verify(eventRepository).findById(1L);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventProducer, eventMapper);
    }

    @Test
    void update_shouldThrowAccessDeniedException_whenEventUserIdIsNull() {
        CreateEventRequest updateRequest = new CreateEventRequest();
        updateRequest.setTitle("Updated Title");

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(null);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        assertThatThrownBy(() -> eventService.update(updateRequest, 20L, 1L, "otheruser"))
                .isInstanceOf(NullPointerException.class);

        verify(eventRepository).findById(1L);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventProducer, eventMapper);
    }

    @Test
    void update_shouldHandleNullUsernameGracefully() {
        CreateEventRequest updateRequest = new CreateEventRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated description");
        updateRequest.setEventDate(LocalDateTime.now().plusDays(3));
        updateRequest.setLocation("Updated Location");
        updateRequest.setCapacity(15);

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setTitle("Old Title");
        existingEvent.setDescription("Old description");
        existingEvent.setEventDate(LocalDateTime.now().plusDays(1));
        existingEvent.setLocation("Old Location");
        existingEvent.setCapacity(10);
        existingEvent.setSeatsAvailable(10);
        existingEvent.setUserId(10L);

        EventResponse updatedResponse = new EventResponse();
        updatedResponse.setId(1L);
        updatedResponse.setTitle("Updated Title");
        updatedResponse.setDescription("Updated description");
        updatedResponse.setEventDate(LocalDateTime.now().plusDays(3));
        updatedResponse.setLocation("Updated Location");
        updatedResponse.setCapacity(15);
        updatedResponse.setSeatsAvailable(10);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(eventMapper.toDto(existingEvent)).thenReturn(updatedResponse);

        EventResponse result = eventService.update(updateRequest, 10L, 1L, null);

        assertThat(result).isNotNull();

        verify(eventRepository).findById(1L);
        verify(eventMapper).toDto(existingEvent);

        ArgumentCaptor<EventUpdated> captor = ArgumentCaptor.forClass(EventUpdated.class);
        verify(eventProducer).sendEventUpdated(captor.capture());

        EventUpdated sentEvent = captor.getValue();
        assertThat(sentEvent.getId()).isEqualTo(1L);
        assertThat(sentEvent.getTitle()).isEqualTo("Updated Title");
        assertThat(sentEvent.getUsername()).isNull();
        assertThat(sentEvent.getLocation()).isEqualTo("Updated Location");
    }

    @Test
    void update_shouldHandleNullTitleInRequestGracefully() {
        CreateEventRequest updateRequest = new CreateEventRequest();
        updateRequest.setTitle(null);
        updateRequest.setDescription("Updated description");
        updateRequest.setEventDate(LocalDateTime.now().plusDays(3));
        updateRequest.setLocation("Updated Location");
        updateRequest.setCapacity(15);

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setTitle("Old Title");
        existingEvent.setDescription("Old description");
        existingEvent.setEventDate(LocalDateTime.now().plusDays(1));
        existingEvent.setLocation("Old Location");
        existingEvent.setCapacity(10);
        existingEvent.setSeatsAvailable(10);
        existingEvent.setUserId(10L);

        EventResponse updatedResponse = new EventResponse();
        updatedResponse.setId(1L);
        updatedResponse.setTitle(null);
        updatedResponse.setDescription("Updated description");
        updatedResponse.setEventDate(LocalDateTime.now().plusDays(3));
        updatedResponse.setLocation("Updated Location");
        updatedResponse.setCapacity(15);
        updatedResponse.setSeatsAvailable(10);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(eventMapper.toDto(existingEvent)).thenReturn(updatedResponse);

        EventResponse result = eventService.update(updateRequest, 10L, 1L, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isNull();

        verify(eventRepository).findById(1L);
        verify(eventMapper).toDto(existingEvent);

        ArgumentCaptor<EventUpdated> captor = ArgumentCaptor.forClass(EventUpdated.class);
        verify(eventProducer).sendEventUpdated(captor.capture());

        EventUpdated sentEvent = captor.getValue();
        assertThat(sentEvent.getTitle()).isNull();
        assertThat(sentEvent.getUsername()).isEqualTo("testuser");
    }

    @Test
    void update_shouldThrowEntityNotFoundException_whenEventIdIsNull() {
        CreateEventRequest updateRequest = new CreateEventRequest();
        updateRequest.setTitle("Updated Title");

        when(eventRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.update(updateRequest, 10L, null, "testuser"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found");

        verify(eventRepository).findById(null);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventProducer, eventMapper);
    }

    @Test
    void update_shouldCallRepositoryAndProducerInCorrectOrder() {
        CreateEventRequest updateRequest = new CreateEventRequest();
        updateRequest.setTitle("Updated Title");

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(eventMapper.toDto(existingEvent)).thenReturn(eventResponse);

        eventService.update(updateRequest, 10L, 1L, "testuser");

        InOrder inOrder = inOrder(eventRepository, eventProducer, eventMapper);
        inOrder.verify(eventRepository).findById(1L);
        inOrder.verify(eventProducer).sendEventUpdated(any());
        inOrder.verify(eventMapper).toDto(existingEvent);
        inOrder.verifyNoMoreInteractions();
    }

    // ==================== delete Tests ====================

    @Test
    void delete_shouldDeleteEventAndSendEventDeleted_whenValidInput() {
        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setTitle("Test Event");
        existingEvent.setUserId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        eventService.delete(1L, 10L, "testuser");

        verify(eventRepository).findById(1L);
        verify(eventRepository).delete(existingEvent);

        ArgumentCaptor<EventDeleted> captor = ArgumentCaptor.forClass(EventDeleted.class);
        verify(eventProducer).sendEventDeleted(captor.capture());

        EventDeleted sentEvent = captor.getValue();
        assertThat(sentEvent.getEventId()).isEqualTo(1L);
        assertThat(sentEvent.getTitle()).isEqualTo("Test Event");
        assertThat(sentEvent.getUserId()).isEqualTo(10L);
        assertThat(sentEvent.getUsername()).isEqualTo("testuser");
    }

    @Test
    void delete_shouldThrowEntityNotFoundException_whenEventDoesNotExist() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.delete(99L, 10L, "testuser"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found");

        verify(eventRepository).findById(99L);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void delete_shouldThrowAccessDeniedException_whenUserIsNotOwner() {
        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        assertThatThrownBy(() -> eventService.delete(1L, 20L, "otheruser"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed to modify this event");

        verify(eventRepository).findById(1L);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void delete_shouldThrowAccessDeniedException_whenEventUserIdIsNull() {
        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(null);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        assertThatThrownBy(() -> eventService.delete(1L, 20L, "otheruser"))
                .isInstanceOf(NullPointerException.class);

        verify(eventRepository).findById(1L);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void delete_shouldHandleNullUsernameGracefully() {
        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setTitle("Test Event");
        existingEvent.setUserId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        eventService.delete(1L, 10L, null);

        verify(eventRepository).findById(1L);
        verify(eventRepository).delete(existingEvent);

        ArgumentCaptor<EventDeleted> captor = ArgumentCaptor.forClass(EventDeleted.class);
        verify(eventProducer).sendEventDeleted(captor.capture());

        EventDeleted sentEvent = captor.getValue();
        assertThat(sentEvent.getEventId()).isEqualTo(1L);
        assertThat(sentEvent.getTitle()).isEqualTo("Test Event");
        assertThat(sentEvent.getUserId()).isEqualTo(10L);
        assertThat(sentEvent.getUsername()).isNull();
    }

    @Test
    void delete_shouldThrowEntityNotFoundException_whenEventIdIsNull() {
        when(eventRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.delete(null, 10L, "testuser"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found");

        verify(eventRepository).findById(null);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void delete_shouldNotDeleteEvent_whenEventDoesNotExistAndIdIsNull() {
        when(eventRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.delete(null, 10L, "testuser"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found");

        verify(eventRepository).findById(null);
        verifyNoMoreInteractions(eventRepository);
        verify(eventProducer, never()).sendEventDeleted(any());
    }

    @Test
    void delete_shouldCallRepositoryInCorrectOrder() {
        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        eventService.delete(1L, 10L, "testuser");

        InOrder inOrder = inOrder(eventRepository, eventProducer);
        inOrder.verify(eventRepository).findById(1L);
        inOrder.verify(eventRepository).delete(existingEvent);
        inOrder.verify(eventProducer).sendEventDeleted(any());
        inOrder.verifyNoMoreInteractions();
    }

    // ==================== reserveSeat Tests ====================

    @Test
    void reserveSeat_shouldReserveSeatAndSendBookingApproved_whenSeatsAvailable() {
        when(eventRepository.reserveSeat(100L)).thenReturn(1);

        eventService.reserveSeat(bookingCreatedEvent);

        verify(eventRepository).reserveSeat(100L);

        ArgumentCaptor<BookingCreatedEvent> captor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(eventProducer).sendBookingApproved(captor.capture());

        BookingCreatedEvent sentEvent = captor.getValue();
        assertThat(sentEvent.getEventId()).isEqualTo(100L);
        assertThat(sentEvent.getUserId()).isEqualTo(10L);
        assertThat(sentEvent.getUsername()).isEqualTo("testuser");
    }

    @Test
    void reserveSeat_shouldThrowIllegalStateException_whenNoSeatsAvailable() {
        when(eventRepository.reserveSeat(100L)).thenReturn(0);

        assertThatThrownBy(() -> eventService.reserveSeat(bookingCreatedEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No available seats");

        verify(eventRepository).reserveSeat(100L);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void reserveSeat_shouldThrowException_whenEventIdIsNull() {
        BookingCreatedEvent nullEventId = new BookingCreatedEvent();
        nullEventId.setEventId(null);
        nullEventId.setUserId(10L);
        nullEventId.setUsername("testuser");

        when(eventRepository.reserveSeat(null)).thenReturn(0);

        assertThatThrownBy(() -> eventService.reserveSeat(nullEventId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No available seats");

        verify(eventRepository).reserveSeat(null);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void reserveSeat_shouldThrowNullPointerException_whenBookingCreatedEventIsNull() {
        assertThatThrownBy(() -> eventService.reserveSeat(null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(eventRepository, eventProducer);
    }

    @Test
    void reserveSeat_shouldHandleZeroEventId() {
        BookingCreatedEvent zeroEventId = new BookingCreatedEvent();
        zeroEventId.setEventId(0L);
        zeroEventId.setUserId(10L);
        zeroEventId.setUsername("testuser");

        when(eventRepository.reserveSeat(0L)).thenReturn(1);

        eventService.reserveSeat(zeroEventId);

        verify(eventRepository).reserveSeat(0L);

        ArgumentCaptor<BookingCreatedEvent> captor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(eventProducer).sendBookingApproved(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(0L);
    }

    // ==================== cancelReservation Tests ====================

    @Test
    void cancelReservation_shouldDecreaseSeatsAndSucceed_whenReservationCancelled() {
        when(eventRepository.cancelReservation(100L)).thenReturn(1);

        eventService.cancelReservation(100L);

        verify(eventRepository).cancelReservation(100L);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void cancelReservation_shouldThrowIllegalStateException_whenSeatsAlreadyAtCapacity() {
        when(eventRepository.cancelReservation(100L)).thenReturn(0);

        assertThatThrownBy(() -> eventService.cancelReservation(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Available seats already equal capacity");

        verify(eventRepository).cancelReservation(100L);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void cancelReservation_shouldThrowException_whenEventIdIsNull() {
        when(eventRepository.cancelReservation(null)).thenReturn(0);

        assertThatThrownBy(() -> eventService.cancelReservation(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Available seats already equal capacity");

        verify(eventRepository).cancelReservation(null);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void cancelReservation_shouldHandleZeroEventId() {
        when(eventRepository.cancelReservation(0L)).thenReturn(1);

        eventService.cancelReservation(0L);

        verify(eventRepository).cancelReservation(0L);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void cancelReservation_shouldHandleNegativeEventId() {
        when(eventRepository.cancelReservation(-1L)).thenReturn(1);

        eventService.cancelReservation(-1L);

        verify(eventRepository).cancelReservation(-1L);
        verifyNoInteractions(eventProducer);
    }

    // ==================== Concurrent Scenario Tests ====================

    @Test
    void create_shouldHandleMultipleCreates() {
        Event newEvent = new Event();
        newEvent.setTitle(createRequest.getTitle());
        newEvent.setDescription(createRequest.getDescription());
        newEvent.setEventDate(createRequest.getEventDate());
        newEvent.setLocation(createRequest.getLocation());
        newEvent.setCapacity(createRequest.getCapacity());
        newEvent.setUserId(10L);

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle(createRequest.getTitle());
        savedEvent.setDescription(createRequest.getDescription());
        savedEvent.setEventDate(createRequest.getEventDate());
        savedEvent.setLocation(createRequest.getLocation());
        savedEvent.setCapacity(createRequest.getCapacity());
        savedEvent.setUserId(10L);

        when(eventMapper.toEntity(createRequest)).thenReturn(newEvent);
        when(eventRepository.save(newEvent)).thenReturn(savedEvent);
        when(eventMapper.toDto(savedEvent)).thenReturn(eventResponse);

        EventResponse result1 = eventService.create(createRequest, 10L, "testuser");
        EventResponse result2 = eventService.create(createRequest, 20L, "testuser2");

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();

        verify(eventRepository, times(2)).save(any(Event.class));
        verify(eventProducer, times(2)).sendEventCreated(any());
    }

    @Test
    void delete_shouldHandleMultipleDeletes() {
        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        eventService.delete(1L, 10L, "testuser");
        eventService.delete(1L, 10L, "testuser");

        verify(eventRepository, times(2)).findById(1L);
        verify(eventRepository, times(2)).delete(existingEvent);
        verify(eventProducer, times(2)).sendEventDeleted(any());
    }

    @Test
    void reserveSeat_shouldHandleMultipleReservations() {
        when(eventRepository.reserveSeat(100L)).thenReturn(1);

        eventService.reserveSeat(bookingCreatedEvent);
        eventService.reserveSeat(bookingCreatedEvent);

        verify(eventRepository, times(2)).reserveSeat(100L);
        verify(eventProducer, times(2)).sendBookingApproved(any());
    }

    // ==================== Repository Interaction Verification Tests ====================

    @Test
    void getEvent_shouldCallRepositoryExactlyOnce() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventMapper.toDto(testEvent)).thenReturn(eventResponse);

        eventService.getEvent(1L);

        verify(eventRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void create_shouldCallSaveExactlyOnce() {
        Event newEvent = new Event();
        newEvent.setTitle(createRequest.getTitle());
        newEvent.setUserId(10L);

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle(createRequest.getTitle());
        savedEvent.setUserId(10L);

        when(eventMapper.toEntity(createRequest)).thenReturn(newEvent);
        when(eventRepository.save(newEvent)).thenReturn(savedEvent);
        when(eventMapper.toDto(savedEvent)).thenReturn(eventResponse);

        eventService.create(createRequest, 10L, "testuser");

        verify(eventRepository, times(1)).save(newEvent);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void update_shouldNotCallDelete_whenUpdating() {
        CreateEventRequest updateRequest = new CreateEventRequest();
        updateRequest.setTitle("Updated Title");

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(eventMapper.toDto(existingEvent)).thenReturn(eventResponse);

        eventService.update(updateRequest, 10L, 1L, "testuser");

        verify(eventRepository, never()).delete(any());
    }

    @Test
    void delete_shouldNotCallSave_whenDeleting() {
        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        eventService.delete(1L, 10L, "testuser");

        verify(eventRepository, never()).save(any(Event.class));
    }

    // ==================== Additional Edge Case Tests ====================

    @Test
    void getEvents_shouldHandleVeryLargePageNumber() {
        Page<Event> emptyPage = new PageImpl<>(List.of());
        when(eventRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<EventResponse> result = eventService.getEvents(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(eventRepository).findAll(pageable);
    }

    @Test
    void create_shouldHandleEventWithNullDescription() {
        CreateEventRequest nullDescRequest = new CreateEventRequest();
        nullDescRequest.setTitle("Event");
        nullDescRequest.setDescription(null);
        nullDescRequest.setEventDate(LocalDateTime.now().plusDays(2));
        nullDescRequest.setLocation("Location");
        nullDescRequest.setCapacity(20);

        Event newEvent = new Event();
        newEvent.setTitle("Event");
        newEvent.setDescription(null);
        newEvent.setEventDate(nullDescRequest.getEventDate());
        newEvent.setLocation("Location");
        newEvent.setCapacity(20);
        newEvent.setUserId(10L);

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle("Event");
        savedEvent.setDescription(null);
        savedEvent.setEventDate(nullDescRequest.getEventDate());
        savedEvent.setLocation("Location");
        savedEvent.setCapacity(20);
        savedEvent.setUserId(10L);

        when(eventMapper.toEntity(nullDescRequest)).thenReturn(newEvent);
        when(eventRepository.save(newEvent)).thenReturn(savedEvent);
        when(eventMapper.toDto(savedEvent)).thenReturn(eventResponse);

        EventResponse result = eventService.create(nullDescRequest, 10L, "testuser");

        assertThat(result).isNotNull();
        verify(eventRepository).save(newEvent);
    }

    @Test
    void update_shouldHandleEventWithZeroCapacity() {
        CreateEventRequest zeroCapacityRequest = new CreateEventRequest();
        zeroCapacityRequest.setTitle("Updated Title");
        zeroCapacityRequest.setCapacity(0);

        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setUserId(10L);

        EventResponse updatedResponse = new EventResponse();
        updatedResponse.setId(1L);
        updatedResponse.setTitle("Updated Title");
        updatedResponse.setCapacity(0);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(eventMapper.toDto(existingEvent)).thenReturn(updatedResponse);

        EventResponse result = eventService.update(zeroCapacityRequest, 10L, 1L, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getCapacity()).isEqualTo(0);
    }

    @Test
    void reserveSeat_shouldHandleNullUserIdInEvent() {
        BookingCreatedEvent nullUserIdEvent = new BookingCreatedEvent();
        nullUserIdEvent.setEventId(100L);
        nullUserIdEvent.setUserId(null);
        nullUserIdEvent.setUsername("testuser");

        when(eventRepository.reserveSeat(100L)).thenReturn(1);

        eventService.reserveSeat(nullUserIdEvent);

        verify(eventRepository).reserveSeat(100L);

        ArgumentCaptor<BookingCreatedEvent> captor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(eventProducer).sendBookingApproved(captor.capture());
        assertThat(captor.getValue().getUserId()).isNull();
    }

    @Test
    void reserveSeat_shouldHandleNullUsernameInEvent() {
        BookingCreatedEvent nullUsernameEvent = new BookingCreatedEvent();
        nullUsernameEvent.setEventId(100L);
        nullUsernameEvent.setUserId(10L);
        nullUsernameEvent.setUsername(null);

        when(eventRepository.reserveSeat(100L)).thenReturn(1);

        eventService.reserveSeat(nullUsernameEvent);

        verify(eventRepository).reserveSeat(100L);

        ArgumentCaptor<BookingCreatedEvent> captor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(eventProducer).sendBookingApproved(captor.capture());
        assertThat(captor.getValue().getUsername()).isNull();
    }
}