package org.example.bookingservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.bookingservice.dto.request.BookingRequest;
import org.example.bookingservice.dto.response.BookingResponse;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.BookingStatus;
import org.example.bookingservice.kafka.producer.BookingEventProducer;
import org.example.bookingservice.mapper.BookingMapper;
import org.example.bookingservice.repository.BookingRepository;
import org.example.common.kafka.event.BookingCancelledEvent;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.BookingDeletedEvent;
import org.example.common.kafka.event.EventDeleted;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private BookingEventProducer bookingEventProducer;

    @Mock
    private Pageable pageable;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Booking testBooking;
    private BookingRequest testBookingRequest;
    private BookingResponse testBookingResponse;
    private BookingCreatedEvent testBookingCreatedEvent;
    private EventDeleted testEventDeleted;

    @BeforeEach
    void setUp() {
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUserId(10L);
        testBooking.setEventId(100L);
        testBooking.setStatus(BookingStatus.PENDING);

        testBookingRequest = new BookingRequest();
        testBookingRequest.setEventId(100L);

        testBookingResponse = new BookingResponse();
        testBookingResponse.setId(1L);
        testBookingResponse.setEventId(100L);
        testBookingResponse.setStatus(BookingStatus.PENDING);

        testBookingCreatedEvent = new BookingCreatedEvent();
        testBookingCreatedEvent.setUserId(10L);
        testBookingCreatedEvent.setEventId(100L);
        testBookingCreatedEvent.setUsername("testuser");

        testEventDeleted = new EventDeleted();
        testEventDeleted.setEventId(100L);
        testEventDeleted.setUserId(10L);
        testEventDeleted.setUsername("testuser");
    }

    // ==================== findAllByUserId Tests ====================

    @Test
    void findAllByUserId_shouldReturnBookings_whenUserHasBookings() {
        List<Booking> bookingsList = new ArrayList<>();
        bookingsList.add(testBooking);
        Page<Booking> bookingPage = new PageImpl<>(bookingsList);

        when(bookingRepository.findAllByUserId(10L, pageable)).thenReturn(bookingPage);
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingResponse);

        Page<BookingResponse> result = bookingService.findAllByUserId(10L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);

        verify(bookingRepository).findAllByUserId(10L, pageable);
        verify(bookingMapper).toDto(testBooking);
    }

    @Test
    void findAllByUserId_shouldReturnEmptyPage_whenUserHasNoBookings() {
        Page<Booking> emptyPage = new PageImpl<>(new ArrayList<>());

        when(bookingRepository.findAllByUserId(10L, pageable)).thenReturn(emptyPage);

        Page<BookingResponse> result = bookingService.findAllByUserId(10L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();

        verify(bookingRepository).findAllByUserId(10L, pageable);
        verify(bookingMapper, never()).toDto(any());
    }

    @Test
    void findAllByUserId_shouldHandleNullUserId() {
        Page<Booking> emptyPage = new PageImpl<>(new ArrayList<>());

        when(bookingRepository.findAllByUserId(eq(null), any(Pageable.class))).thenReturn(emptyPage);

        Page<BookingResponse> result = bookingService.findAllByUserId(null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(bookingRepository).findAllByUserId(null, pageable);
    }

    // ==================== findById Tests ====================

    @Test
    void findById_shouldReturnBooking_whenExistsAndUserMatches() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingResponse);

        BookingResponse result = bookingService.findById(1L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEventId()).isEqualTo(100L);

        verify(bookingRepository).findById(1L);
        verify(bookingMapper).toDto(testBooking);
    }

    @Test
    void findById_shouldThrowEntityNotFoundException_whenBookingDoesNotExist() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findById(999L, 10L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Booking not found");

        verify(bookingRepository).findById(999L);
        verifyNoInteractions(bookingMapper);
    }

    @Test
    void findById_shouldThrowAccessDeniedException_whenUserDoesNotOwnBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.findById(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed to access this booking");

        verify(bookingRepository).findById(1L);
        verify(bookingMapper, never()).toDto(any());
    }

    @Test
    void findById_shouldThrowNullPointerException_whenBookingUserIdIsNull() {
        testBooking.setUserId(null);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.findById(1L, 10L))
                .isInstanceOf(NullPointerException.class);

        verify(bookingRepository).findById(1L);
    }

    // ==================== create Tests ====================

    @Test
    void create_shouldSaveBookingAndSendEvent_whenValidRequest() {
        Booking savedBooking = new Booking();
        savedBooking.setId(1L);
        savedBooking.setUserId(10L);
        savedBooking.setEventId(100L);
        savedBooking.setStatus(BookingStatus.PENDING);

        when(bookingMapper.toEntity(testBookingRequest)).thenReturn(testBooking);
        when(bookingRepository.save(testBooking)).thenReturn(savedBooking);
        when(bookingMapper.toDto(savedBooking)).thenReturn(testBookingResponse);

        BookingResponse result = bookingService.create(testBookingRequest, 10L, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(bookingMapper).toEntity(testBookingRequest);
        verify(bookingRepository).save(testBooking);
        assertThat(testBooking.getUserId()).isEqualTo(10L);
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingMapper).toDto(savedBooking);

        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(bookingEventProducer).sendBookingCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(100L);
        assertThat(eventCaptor.getValue().getUsername()).isEqualTo("testuser");
    }

    @Test
    void create_shouldSetStatusPending_whenCalled() {
        when(bookingMapper.toEntity(testBookingRequest)).thenReturn(testBooking);
        when(bookingRepository.save(testBooking)).thenReturn(testBooking);
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingResponse);

        BookingResponse result = bookingService.create(testBookingRequest, 10L, "testuser");

        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(bookingRepository).save(testBooking);
    }

    @Test
    void create_shouldNotSaveOrSendEvent_whenEventIdIsNull() {
        BookingRequest nullEventIdRequest = new BookingRequest();
        nullEventIdRequest.setEventId(null);
        when(bookingMapper.toEntity(nullEventIdRequest)).thenReturn(testBooking);
        when(bookingRepository.save(testBooking)).thenReturn(testBooking);
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingResponse);

        bookingService.create(nullEventIdRequest, 10L, "testuser");

        verify(bookingRepository).save(testBooking);
        verify(bookingMapper).toDto(testBooking);
        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(bookingEventProducer).sendBookingCreated(eventCaptor.capture());
    }

    // ==================== delete Tests ====================

    @Test
    void delete_shouldDeleteBookingAndSendEvent_whenUserOwnsBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        doNothing().when(bookingRepository).delete(testBooking);

        bookingService.delete(1L, 10L, "testuser");

        verify(bookingRepository).findById(1L);
        verify(bookingRepository).delete(testBooking);

        ArgumentCaptor<BookingDeletedEvent> eventCaptor = ArgumentCaptor.forClass(BookingDeletedEvent.class);
        verify(bookingEventProducer).sendBookingDeleted(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().getUsername()).isEqualTo("testuser");
    }

    @Test
    void delete_shouldThrowEntityNotFoundException_whenBookingDoesNotExist() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.delete(999L, 10L, "testuser"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Booking not found");

        verify(bookingRepository).findById(999L);
        verify(bookingRepository, never()).delete(any());
        verifyNoInteractions(bookingEventProducer);
    }

    @Test
    void delete_shouldThrowAccessDeniedException_whenUserDoesNotOwnBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.delete(1L, 99L, "testuser"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed to access this booking");

        verify(bookingRepository).findById(1L);
        verify(bookingRepository, never()).delete(any());
        verifyNoInteractions(bookingEventProducer);
    }

    @Test
    void delete_shouldNotSendEvent_whenBookingNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.delete(999L, 10L, "testuser"))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(bookingEventProducer);
    }

    // ==================== approve Tests ====================

    @Test
    void approve_shouldUpdateBookingStatusToConfirmed_whenBookingExists() {
        when(bookingRepository.findByUserIdAndEventId(10L, 100L)).thenReturn(Optional.of(testBooking));

        bookingService.approve(testBookingCreatedEvent);

        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        verify(bookingRepository).findByUserIdAndEventId(10L, 100L);
    }

    @Test
    void approve_shouldNotCallSave_whenApproving() {
        when(bookingRepository.findByUserIdAndEventId(10L, 100L)).thenReturn(Optional.of(testBooking));

        bookingService.approve(testBookingCreatedEvent);

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void approve_shouldThrowEntityNotFoundException_whenBookingDoesNotExist() {
        when(bookingRepository.findByUserIdAndEventId(10L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.approve(testBookingCreatedEvent))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Booking not found");

        verify(bookingRepository).findByUserIdAndEventId(10L, 100L);
    }

    // ==================== setCancelledByEvent Tests ====================

    @Test
    void setCancelledByEvent_shouldUpdateStatusAndSendEvent_whenEventIsDeleted() {
        doNothing().when(bookingRepository).updateStatusByEventId(100L, BookingStatus.CANCELLED);

        bookingService.setCancelledByEvent(testEventDeleted);

        verify(bookingRepository).updateStatusByEventId(100L, BookingStatus.CANCELLED);

        ArgumentCaptor<BookingCancelledEvent> eventCaptor = ArgumentCaptor.forClass(BookingCancelledEvent.class);
        verify(bookingEventProducer).sendBookingCancelled(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(100L);
        assertThat(eventCaptor.getValue().getUsername()).isEqualTo("testuser");
    }

    @Test
    void setCancelledByEvent_shouldCancelWithNullEventId() {
        EventDeleted nullEventId = new EventDeleted();
        nullEventId.setEventId(null);
        nullEventId.setUserId(10L);
        nullEventId.setUsername("testuser");

        doNothing().when(bookingRepository).updateStatusByEventId(eq(null), any(BookingStatus.class));

        bookingService.setCancelledByEvent(nullEventId);

        verify(bookingRepository).updateStatusByEventId(null, BookingStatus.CANCELLED);

        ArgumentCaptor<BookingCancelledEvent> eventCaptor = ArgumentCaptor.forClass(BookingCancelledEvent.class);
        verify(bookingEventProducer).sendBookingCancelled(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isNull();
    }

    @Test
    void setCancelledByEvent_shouldSendBookingCancelledEventWithCorrectPayload() {
        EventDeleted event = new EventDeleted();
        event.setEventId(200L);
        event.setUserId(20L);
        event.setUsername("anotheruser");

        doNothing().when(bookingRepository).updateStatusByEventId(200L, BookingStatus.CANCELLED);

        bookingService.setCancelledByEvent(event);

        verify(bookingRepository).updateStatusByEventId(200L, BookingStatus.CANCELLED);

        ArgumentCaptor<BookingCancelledEvent> eventCaptor = ArgumentCaptor.forClass(BookingCancelledEvent.class);
        verify(bookingEventProducer).sendBookingCancelled(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(20L);
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(200L);
        assertThat(eventCaptor.getValue().getUsername()).isEqualTo("anotheruser");
    }

    // ==================== Additional Edge Case Tests for approve() ====================

    @Test
    void approve_shouldThrowNullPointerException_whenEventIsNull() {
        assertThatThrownBy(() -> bookingService.approve(null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(bookingRepository, bookingEventProducer);
    }

    @Test
    void approve_shouldThrowEntityNotFoundException_whenEventUserIdIsNull() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setEventId(100L);
        event.setUserId(null);
        event.setUsername("testuser");

        when(bookingRepository.findByUserIdAndEventId(null, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.approve(event))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Booking not found");

        verify(bookingRepository).findByUserIdAndEventId(null, 100L);
    }

    @Test
    void approve_shouldThrowEntityNotFoundException_whenEventEventIdIsNull() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setEventId(null);
        event.setUserId(10L);
        event.setUsername("testuser");

        when(bookingRepository.findByUserIdAndEventId(10L, null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.approve(event))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Booking not found");

        verify(bookingRepository).findByUserIdAndEventId(10L, null);
    }

    @Test
    void approve_shouldNotSendKafkaEvent_whenApproving() {
        when(bookingRepository.findByUserIdAndEventId(10L, 100L)).thenReturn(Optional.of(testBooking));

        bookingService.approve(testBookingCreatedEvent);

        verifyNoInteractions(bookingEventProducer);
    }

    // ==================== Additional Edge Case Tests for create() ====================

    @Test
    void create_shouldThrowNullPointerException_whenRequestIsNull() {
        assertThatThrownBy(() -> bookingService.create(null, 10L, "testuser"))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(bookingRepository, bookingMapper, bookingEventProducer);
    }

    @Test
    void create_shouldHandleNullUserIdGracefully() {
        when(bookingMapper.toEntity(testBookingRequest)).thenReturn(testBooking);
        when(bookingRepository.save(testBooking)).thenReturn(testBooking);
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingResponse);

        BookingResponse result = bookingService.create(testBookingRequest, null, "testuser");

        assertThat(result).isNotNull();
        assertThat(testBooking.getUserId()).isNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(bookingMapper).toEntity(testBookingRequest);
        verify(bookingRepository).save(testBooking);
        verify(bookingMapper).toDto(testBooking);

        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(bookingEventProducer).sendBookingCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isNull();
    }

    @Test
    void create_shouldHandleNullUsernameGracefully() {
        when(bookingMapper.toEntity(testBookingRequest)).thenReturn(testBooking);
        when(bookingRepository.save(testBooking)).thenReturn(testBooking);
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingResponse);

        BookingResponse result = bookingService.create(testBookingRequest, 10L, null);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(bookingRepository).save(testBooking);

        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(bookingEventProducer).sendBookingCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(100L);
        assertThat(eventCaptor.getValue().getUsername()).isNull();
    }

    @Test
    void create_shouldSendBookingCreatedEventWithCorrectPayload() {
        when(bookingMapper.toEntity(testBookingRequest)).thenReturn(testBooking);
        when(bookingRepository.save(testBooking)).thenReturn(testBooking);
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingResponse);

        bookingService.create(testBookingRequest, 10L, "testuser");

        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(bookingEventProducer).sendBookingCreated(eventCaptor.capture());
        BookingCreatedEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent).isNotNull();
        assertThat(sentEvent.getUserId()).isEqualTo(10L);
        assertThat(sentEvent.getEventId()).isEqualTo(100L);
        assertThat(sentEvent.getUsername()).isEqualTo("testuser");
    }

    @Test
    void create_shouldNotSendEvent_whenEventIdIsNullInRequest() {
        BookingRequest nullEventIdRequest = new BookingRequest();
        nullEventIdRequest.setEventId(null);
        
        Booking bookingWithNullEventId = new Booking();
        bookingWithNullEventId.setId(1L);
        bookingWithNullEventId.setUserId(10L);
        bookingWithNullEventId.setEventId(null);
        bookingWithNullEventId.setStatus(BookingStatus.PENDING);
        
        BookingResponse responseWithNullEventId = new BookingResponse();
        responseWithNullEventId.setId(1L);
        responseWithNullEventId.setEventId(null);
        responseWithNullEventId.setStatus(BookingStatus.PENDING);
        
        when(bookingMapper.toEntity(nullEventIdRequest)).thenReturn(bookingWithNullEventId);
        when(bookingRepository.save(bookingWithNullEventId)).thenReturn(bookingWithNullEventId);
        when(bookingMapper.toDto(bookingWithNullEventId)).thenReturn(responseWithNullEventId);

        BookingResponse result = bookingService.create(nullEventIdRequest, 10L, "testuser");

        verify(bookingRepository).save(bookingWithNullEventId);
        verify(bookingMapper).toDto(bookingWithNullEventId);

        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(bookingEventProducer).sendBookingCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isNull();
        assertThat(result.getEventId()).isNull();
    }

    // ==================== Additional Edge Case Tests for findAllByUserId() ====================

    @Test
    void findAllByUserId_shouldHandleNegativeUserId() {
        Page<Booking> emptyPage = new PageImpl<>(new ArrayList<>());

        when(bookingRepository.findAllByUserId(-1L, pageable)).thenReturn(emptyPage);

        Page<BookingResponse> result = bookingService.findAllByUserId(-1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(bookingRepository).findAllByUserId(-1L, pageable);
    }

    @Test
    void findAllByUserId_shouldHandleZeroUserId() {
        Page<Booking> emptyPage = new PageImpl<>(new ArrayList<>());

        when(bookingRepository.findAllByUserId(0L, pageable)).thenReturn(emptyPage);

        Page<BookingResponse> result = bookingService.findAllByUserId(0L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(bookingRepository).findAllByUserId(0L, pageable);
    }

    // ==================== Additional Edge Case Tests for findById() ====================

    @Test
    void findById_shouldThrowEntityNotFoundException_whenIdIsNull() {
        when(bookingRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findById(null, 10L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Booking not found");

        verify(bookingRepository).findById(null);
        verifyNoInteractions(bookingMapper);
    }

    // ==================== Additional Edge Case Tests for delete() ====================

    @Test
    void delete_shouldThrowEntityNotFoundException_whenIdIsNull() {
        when(bookingRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.delete(null, 10L, "testuser"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Booking not found");

        verify(bookingRepository).findById(null);
        verifyNoInteractions(bookingEventProducer);
    }

    @Test
    void delete_shouldNotSendEvent_whenIdIsNull() {
        when(bookingRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.delete(null, 10L, "testuser"))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(bookingEventProducer);
    }

    // ==================== Repository Interaction Verification Tests ====================

    @Test
    void create_shouldCallRepositoryAndMapperInCorrectOrder() {
        when(bookingMapper.toEntity(testBookingRequest)).thenReturn(testBooking);
        when(bookingRepository.save(testBooking)).thenReturn(testBooking);
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingResponse);

        bookingService.create(testBookingRequest, 10L, "testuser");

        InOrder inOrder = inOrder(bookingMapper, bookingRepository, bookingEventProducer);
        inOrder.verify(bookingMapper).toEntity(testBookingRequest);
        inOrder.verify(bookingRepository).save(testBooking);
        inOrder.verify(bookingEventProducer).sendBookingCreated(any());
        inOrder.verify(bookingMapper).toDto(testBooking);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void delete_shouldCallRepositoryInCorrectOrder() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        doNothing().when(bookingRepository).delete(testBooking);

        bookingService.delete(1L, 10L, "testuser");

        InOrder inOrder = inOrder(bookingRepository, bookingEventProducer);
        inOrder.verify(bookingRepository).findById(1L);
        inOrder.verify(bookingRepository).delete(testBooking);
        inOrder.verify(bookingEventProducer).sendBookingDeleted(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void approve_shouldCallRepositoryInCorrectOrder() {
        when(bookingRepository.findByUserIdAndEventId(10L, 100L)).thenReturn(Optional.of(testBooking));

        bookingService.approve(testBookingCreatedEvent);

        InOrder inOrder = inOrder(bookingRepository);
        inOrder.verify(bookingRepository).findByUserIdAndEventId(10L, 100L);
        inOrder.verifyNoMoreInteractions();
    }

    // ==================== Concurrent Scenario Tests ====================

    @Test
    void create_shouldHandleMultipleCreates() {
        when(bookingMapper.toEntity(testBookingRequest)).thenReturn(testBooking);
        when(bookingRepository.save(testBooking)).thenReturn(testBooking);
        when(bookingMapper.toDto(testBooking)).thenReturn(testBookingResponse);

        BookingResponse result1 = bookingService.create(testBookingRequest, 10L, "testuser");
        BookingResponse result2 = bookingService.create(testBookingRequest, 20L, "testuser2");

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        verify(bookingRepository, times(2)).save(any(Booking.class));
        verify(bookingEventProducer, times(2)).sendBookingCreated(any());
    }

    @Test
    void delete_shouldHandleMultipleDeletes() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        doNothing().when(bookingRepository).delete(testBooking);

        bookingService.delete(1L, 10L, "testuser");
        bookingService.delete(1L, 10L, "testuser");

        verify(bookingRepository, times(2)).findById(1L);
        verify(bookingRepository, times(2)).delete(testBooking);
        verify(bookingEventProducer, times(2)).sendBookingDeleted(any());
    }

    @Test
    void approve_shouldHandleMultipleApproves() {
        when(bookingRepository.findByUserIdAndEventId(10L, 100L)).thenReturn(Optional.of(testBooking));

        bookingService.approve(testBookingCreatedEvent);
        bookingService.approve(testBookingCreatedEvent);

        verify(bookingRepository, times(2)).findByUserIdAndEventId(10L, 100L);
    }
}
