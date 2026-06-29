package org.example.bookingservice.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.service.BookingService;
import org.example.common.correlation.CorrelationConstants;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.EventDeleted;
import org.example.common.kafka.event.UserDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingEventConsumer bookingEventConsumer;

    private static final String TEST_CORRELATION_ID = "test-correlation-id-123";
    private static final String TEST_TOPIC = "test-topic";
    private static final int TEST_PARTITION = 0;
    private static final long TEST_OFFSET = 0L;
    private static final String TEST_KEY = "test-key";

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    void handleUserDeleted_WithValidEvent_ShouldDeleteBookingsByUserId() {
        // Arrange
        Long userId = 1L;
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(userId)
                .build();

        ConsumerRecord<String, UserDeletedEvent> record = new ConsumerRecord<>(
                TEST_TOPIC,
                TEST_PARTITION,
                TEST_OFFSET,
                TEST_KEY,
                event
        );

        // Act
        bookingEventConsumer.handleUserDeleted(record);

        // Assert
        verify(bookingRepository, times(1)).deleteAllByUserId(userId);
        verifyNoMoreInteractions(bookingRepository);
        verifyNoInteractions(bookingService);
    }

    @Test
    void handleUserDeleted_WithNullUserId_ShouldHandleGracefully() {
        // Arrange
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(null)
                .build();

        ConsumerRecord<String, UserDeletedEvent> record = new ConsumerRecord<>(
                TEST_TOPIC,
                TEST_PARTITION,
                TEST_OFFSET,
                TEST_KEY,
                event
        );

        // Act & Assert - should not throw NullPointerException
        bookingEventConsumer.handleUserDeleted(record);

        verify(bookingRepository, times(1)).deleteAllByUserId(null);
    }

    @Test
    void handleUserDeleted_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        Long userId = 1L;
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(userId)
                .build();

        ConsumerRecord<String, UserDeletedEvent> record = new ConsumerRecord<>(
                TEST_TOPIC,
                TEST_PARTITION,
                TEST_OFFSET,
                TEST_KEY,
                event
        );

        doThrow(new RuntimeException("Database error"))
                .when(bookingRepository).deleteAllByUserId(userId);

        // Act & Assert
        try {
            bookingEventConsumer.handleUserDeleted(record);
        } catch (RuntimeException e) {
            // Exception should be propagated for ErrorHandler to catch
            verify(bookingRepository, times(1)).deleteAllByUserId(userId);
        }
    }

    @Test
    void handleEventDeleted_WithValidEvent_ShouldCancelBookingsByEvent() {
        // Arrange
        Long eventId = 10L;
        Long userId = 5L;
        String username = "testuser";
        String title = "Test Event";

        EventDeleted event = EventDeleted.builder()
                .userId(userId)
                .username(username)
                .eventId(eventId)
                .title(title)
                .build();

        ConsumerRecord<String, EventDeleted> record = new ConsumerRecord<>(
                TEST_TOPIC,
                TEST_PARTITION,
                TEST_OFFSET,
                TEST_KEY,
                event
        );

        // Act
        bookingEventConsumer.handleEventDeleted(record);

        // Assert
        verify(bookingService, times(1)).setCancelledByEvent(event);
        verifyNoMoreInteractions(bookingService);
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void handleEventDeleted_WithNullFields_ShouldHandleGracefully() {
        // Arrange
        EventDeleted event = new EventDeleted();

        ConsumerRecord<String, EventDeleted> record = new ConsumerRecord<>(
                TEST_TOPIC,
                TEST_PARTITION,
                TEST_OFFSET,
                TEST_KEY,
                event
        );

        // Act & Assert - should not throw NullPointerException
        bookingEventConsumer.handleEventDeleted(record);

        verify(bookingService, times(1)).setCancelledByEvent(event);
    }

    @Test
    void handleEventDeleted_WhenServiceThrowsException_ShouldPropagateException() {
        // Arrange
        Long eventId = 10L;
        EventDeleted event = EventDeleted.builder()
                .eventId(eventId)
                .build();

        ConsumerRecord<String, EventDeleted> record = new ConsumerRecord<>(
                TEST_TOPIC,
                TEST_PARTITION,
                TEST_OFFSET,
                TEST_KEY,
                event
        );

        doThrow(new RuntimeException("Service error"))
                .when(bookingService).setCancelledByEvent(event);

        // Act & Assert
        try {
            bookingEventConsumer.handleEventDeleted(record);
        } catch (RuntimeException e) {
            // Exception should be propagated for ErrorHandler to catch
            verify(bookingService, times(1)).setCancelledByEvent(event);
        }
    }

    @Test
    void handleApproved_WithValidEvent_ShouldApproveBooking() {
        // Arrange
        Long userId = 1L;
        Long eventId = 10L;
        String username = "testuser";

        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(userId)
                .eventId(eventId)
                .username(username)
                .build();

        ConsumerRecord<String, BookingCreatedEvent> record = new ConsumerRecord<>(
                TEST_TOPIC,
                TEST_PARTITION,
                TEST_OFFSET,
                TEST_KEY,
                event
        );

        // Act
        bookingEventConsumer.handleApproved(record);

        // Assert
        verify(bookingService, times(1)).approve(event);
        verifyNoMoreInteractions(bookingService);
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void handleApproved_WithNullFields_ShouldHandleGracefully() {
        // Arrange
        BookingCreatedEvent event = new BookingCreatedEvent();

        ConsumerRecord<String, BookingCreatedEvent> record = new ConsumerRecord<>(
                TEST_TOPIC,
                TEST_PARTITION,
                TEST_OFFSET,
                TEST_KEY,
                event
        );

        // Act & Assert - should not throw NullPointerException
        bookingEventConsumer.handleApproved(record);

        verify(bookingService, times(1)).approve(event);
    }

    @Test
    void handleApproved_WhenServiceThrowsException_ShouldPropagateException() {
        // Arrange
        Long userId = 1L;
        Long eventId = 10L;
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(userId)
                .eventId(eventId)
                .build();

        ConsumerRecord<String, BookingCreatedEvent> record = new ConsumerRecord<>(
                TEST_TOPIC,
                TEST_PARTITION,
                TEST_OFFSET,
                TEST_KEY,
                event
        );

        doThrow(new RuntimeException("Service error"))
                .when(bookingService).approve(event);

        // Act & Assert
        try {
            bookingEventConsumer.handleApproved(record);
        } catch (RuntimeException e) {
            // Exception should be propagated for ErrorHandler to catch
            verify(bookingService, times(1)).approve(event);
        }
    }

    @Test
    void handleUserDeleted_WithCorrelationId_ShouldAddToMdc() {
        // Arrange
        Long userId = 1L;
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(userId)
                .build();

        // Use mocked ConsumerRecord to avoid constructor type inference issues
        @SuppressWarnings("unchecked")
        ConsumerRecord<String, UserDeletedEvent> record = mock(ConsumerRecord.class);
        when(record.value()).thenReturn(event);

        RecordHeaders headers = new RecordHeaders();
        headers.add(new RecordHeader(
                CorrelationConstants.CORRELATION_ID,
                TEST_CORRELATION_ID.getBytes(StandardCharsets.UTF_8)
        ));
        when(record.headers()).thenReturn(headers);

        // Act
        bookingEventConsumer.handleUserDeleted(record);

        // Assert
        verify(bookingRepository, times(1)).deleteAllByUserId(userId);
        // MDC should be cleared in finally block
        assert MDC.get(CorrelationConstants.MDC_KEY) == null;
    }

    @Test
    void handleEventDeleted_WithCorrelationId_ShouldAddToMdc() {
        // Arrange
        Long eventId = 10L;
        EventDeleted event = EventDeleted.builder()
                .eventId(eventId)
                .build();

        // Use mocked ConsumerRecord to avoid constructor type inference issues
        @SuppressWarnings("unchecked")
        ConsumerRecord<String, EventDeleted> record = mock(ConsumerRecord.class);
        when(record.value()).thenReturn(event);

        RecordHeaders headers = new RecordHeaders();
        headers.add(new RecordHeader(
                CorrelationConstants.CORRELATION_ID,
                TEST_CORRELATION_ID.getBytes(StandardCharsets.UTF_8)
        ));
        when(record.headers()).thenReturn(headers);

        // Act
        bookingEventConsumer.handleEventDeleted(record);

        // Assert
        verify(bookingService, times(1)).setCancelledByEvent(event);
        // MDC should be cleared in finally block
        assert MDC.get(CorrelationConstants.MDC_KEY) == null;
    }

    @Test
    void handleApproved_WithCorrelationId_ShouldAddToMdc() {
        // Arrange
        Long userId = 1L;
        Long eventId = 10L;
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(userId)
                .eventId(eventId)
                .build();

        // Use mocked ConsumerRecord to avoid constructor type inference issues
        @SuppressWarnings("unchecked")
        ConsumerRecord<String, BookingCreatedEvent> record = mock(ConsumerRecord.class);
        when(record.value()).thenReturn(event);

        RecordHeaders headers = new RecordHeaders();
        headers.add(new RecordHeader(
                CorrelationConstants.CORRELATION_ID,
                TEST_CORRELATION_ID.getBytes(StandardCharsets.UTF_8)
        ));
        when(record.headers()).thenReturn(headers);

        // Act
        bookingEventConsumer.handleApproved(record);

        // Assert
        verify(bookingService, times(1)).approve(event);
        // MDC should be cleared in finally block
        assert MDC.get(CorrelationConstants.MDC_KEY) == null;
    }
}
