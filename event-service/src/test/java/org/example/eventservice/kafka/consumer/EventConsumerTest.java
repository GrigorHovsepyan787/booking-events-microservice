package org.example.eventservice.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.example.common.correlation.CorrelationConstants;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.BookingDeletedEvent;
import org.example.common.kafka.event.UserDeletedEvent;
import org.example.eventservice.repository.EventRepository;
import org.example.eventservice.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ConsumerRecord<String, BookingCreatedEvent> bookingCreatedRecord;

    @Mock
    private ConsumerRecord<String, BookingDeletedEvent> bookingDeletedRecord;

    @Mock
    private ConsumerRecord<String, UserDeletedEvent> userDeletedRecord;

    @Mock
    private Header correlationHeader;

    @Mock
    private org.apache.kafka.common.header.Headers headers;

    @InjectMocks
    private EventConsumer eventConsumer;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    // ==================== Happy Path Tests ====================

    @Test
    void handleBookingCreated_shouldCallReserveSeat() {
        // Arrange
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(1L)
                .eventId(10L)
                .username("testuser")
                .build();

        when(bookingCreatedRecord.value()).thenReturn(event);
        when(bookingCreatedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(correlationHeader);
        when(correlationHeader.value()).thenReturn("test-correlation-id".getBytes(StandardCharsets.UTF_8));

        // Act
        eventConsumer.handleBookingCreated(bookingCreatedRecord);

        // Assert
        verify(eventService, times(1)).reserveSeat(event);
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared after processing");
    }

    @Test
    void handleBookingDeleted_shouldCallCancelReservation() {
        // Arrange
        BookingDeletedEvent event = new BookingDeletedEvent();
        event.setUserId(1L);
        event.setEventId(10L);

        when(bookingDeletedRecord.value()).thenReturn(event);
        when(bookingDeletedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(correlationHeader);
        when(correlationHeader.value()).thenReturn("test-correlation-id".getBytes(StandardCharsets.UTF_8));

        // Act
        eventConsumer.handleBookingDeleted(bookingDeletedRecord);

        // Assert
        verify(eventService, times(1)).cancelReservation(10L);
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared after processing");
    }

    @Test
    void handleUserDeleted_shouldCallDeleteAllByUserId() {
        // Arrange
        UserDeletedEvent event = new UserDeletedEvent();
        event.setUserId(1L);

        when(userDeletedRecord.value()).thenReturn(event);
        when(userDeletedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(correlationHeader);
        when(correlationHeader.value()).thenReturn("test-correlation-id".getBytes(StandardCharsets.UTF_8));

        // Act
        eventConsumer.handleUserDeleted(userDeletedRecord);

        // Assert
        verify(eventRepository, times(1)).deleteAllByUserId(1L);
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared after processing");
    }

    // ==================== Error Handling Tests ====================

    @Test
    void handleBookingCreated_whenServiceThrowsException_shouldPropagateException() {
        // Arrange
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(1L)
                .eventId(10L)
                .username("testuser")
                .build();

        when(bookingCreatedRecord.value()).thenReturn(event);
        when(bookingCreatedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(correlationHeader);
        when(correlationHeader.value()).thenReturn("test-correlation-id".getBytes(StandardCharsets.UTF_8));

        doThrow(new RuntimeException("Service error"))
                .when(eventService).reserveSeat(any(BookingCreatedEvent.class));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> eventConsumer.handleBookingCreated(bookingCreatedRecord));

        assertEquals("Service error", thrown.getMessage());
        verify(eventService, times(1)).reserveSeat(event);
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared even when exception occurs");
    }

    @Test
    void handleUserDeleted_whenRepositoryThrowsException_shouldPropagateException() {
        // Arrange
        UserDeletedEvent event = new UserDeletedEvent();
        event.setUserId(1L);

        when(userDeletedRecord.value()).thenReturn(event);
        when(userDeletedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(correlationHeader);
        when(correlationHeader.value()).thenReturn("test-correlation-id".getBytes(StandardCharsets.UTF_8));

        doThrow(new RuntimeException("Repository error"))
                .when(eventRepository).deleteAllByUserId(any(Long.class));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> eventConsumer.handleUserDeleted(userDeletedRecord));

        assertEquals("Repository error", thrown.getMessage());
        verify(eventRepository, times(1)).deleteAllByUserId(1L);
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared even when exception occurs");
    }

    // ==================== Edge Case Tests ====================

    @Test
    void handleBookingCreated_withNullEvent_shouldHandleGracefully() {
        // Arrange
        when(bookingCreatedRecord.value()).thenReturn(null);
        when(bookingCreatedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(null);

        // Act & Assert - should not throw NullPointerException
        assertDoesNotThrow(() -> eventConsumer.handleBookingCreated(bookingCreatedRecord));

        verify(eventService, never()).reserveSeat(any(BookingCreatedEvent.class));
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared after processing");
    }

    @Test
    void handleBookingDeleted_withNullEvent_shouldHandleGracefully() {
        // Arrange
        when(bookingDeletedRecord.value()).thenReturn(null);
        when(bookingDeletedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(null);

        // Act & Assert - should not throw NullPointerException
        assertDoesNotThrow(() -> eventConsumer.handleBookingDeleted(bookingDeletedRecord));

        verify(eventService, never()).cancelReservation(any(Long.class));
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared after processing");
    }

    @Test
    void handleUserDeleted_withNullEvent_shouldHandleGracefully() {
        // Arrange
        when(userDeletedRecord.value()).thenReturn(null);
        when(userDeletedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(null);

        // Act & Assert - should not throw NullPointerException
        assertDoesNotThrow(() -> {
            eventConsumer.handleUserDeleted(userDeletedRecord);
        });

        verify(eventRepository, never()).deleteAllByUserId(any(Long.class));
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared after processing");
    }

    @Test
    void handleBookingCreated_withNullCorrelationHeader_shouldHandleGracefully() {
        // Arrange
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(1L)
                .eventId(10L)
                .username("testuser")
                .build();

        when(bookingCreatedRecord.value()).thenReturn(event);
        when(bookingCreatedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(null);

        // Act
        eventConsumer.handleBookingCreated(bookingCreatedRecord);

        // Assert
        verify(eventService, times(1)).reserveSeat(event);
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared after processing");
    }

    @Test
    void handleBookingCreated_whenExceptionOccurs_mdcShouldStillBeCleared() {
        // Arrange
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(1L)
                .eventId(10L)
                .username("testuser")
                .build();

        when(bookingCreatedRecord.value()).thenReturn(event);
        when(bookingCreatedRecord.headers()).thenReturn(headers);
        when(headers.lastHeader(CorrelationConstants.CORRELATION_ID)).thenReturn(correlationHeader);
        when(correlationHeader.value()).thenReturn("test-correlation-id".getBytes(StandardCharsets.UTF_8));

        doThrow(new RuntimeException("Service error"))
                .when(eventService).reserveSeat(any(BookingCreatedEvent.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> eventConsumer.handleBookingCreated(bookingCreatedRecord));

        // Verify MDC is cleared even after exception
        assertNull(MDC.get(CorrelationConstants.MDC_KEY), "MDC should be cleared in finally block even when exception occurs");
    }
}