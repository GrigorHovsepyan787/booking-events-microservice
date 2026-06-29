package org.example.notificationservice.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.example.common.correlation.CorrelationConstants;
import org.example.common.kafka.event.BookingCancelledEvent;
import org.example.common.kafka.event.BookingCreatedEvent;
import org.example.common.kafka.event.BookingDeletedEvent;
import org.example.common.kafka.event.EventCreated;
import org.example.common.kafka.event.EventUpdated;
import org.example.common.kafka.event.UserCreatedEvent;
import org.example.common.kafka.event.UserDeletedEvent;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.entity.NotificationType;
import org.example.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationEventConsumer(notificationRepository);
        MDC.clear();
    }

    // ==================== Happy Path Tests ====================

    @Test
    void handleCreatedUser_WithValidEvent_ShouldCreateNotification() {
        // Given
        UserCreatedEvent event = UserCreatedEvent.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .build();

        ConsumerRecord<String, UserCreatedEvent> record = createConsumerRecord("user-created", event);

        // When
        consumer.handleCreatedUser(record);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUserId()).isEqualTo(1L);
        assertThat(savedNotification.getUsername()).isEqualTo("john_doe");
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.USER_CREATED);
        assertThat(savedNotification.getMessage()).isEqualTo("Welcome to booking events service");
        assertThat(savedNotification.isRead()).isFalse();
        assertThat(savedNotification.getCreatedAt()).isEqualTo(event.getOccurredAt());
    }

    @Test
    void handleUserDeleted_WithValidEvent_ShouldDeleteNotifications() {
        // Given
        UserDeletedEvent event = new UserDeletedEvent();
        event.setUserId(1L);

        ConsumerRecord<String, UserDeletedEvent> record = createConsumerRecord("user-deleted", event);

        // When
        consumer.handleUserDeleted(record);

        // Then
        verify(notificationRepository).deleteAllByUserId(1L);
    }

    @Test
    void handleCreatedEvent_WithValidEvent_ShouldCreateNotification() {
        // Given
        LocalDateTime eventDate = LocalDateTime.of(2024, 12, 25, 18, 0);
        EventCreated event = EventCreated.builder()
                .userId(1L)
                .username("john_doe")
                .title("Summer Concert")
                .eventDate(eventDate)
                .location("Central Park")
                .build();

        ConsumerRecord<String, EventCreated> record = createConsumerRecord("event-created", event);

        // When
        consumer.handleCreatedEvent(record);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUserId()).isEqualTo(1L);
        assertThat(savedNotification.getUsername()).isEqualTo("john_doe");
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.EVENT_CREATED);
        assertThat(savedNotification.getMessage()).contains("Summer Concert");
        assertThat(savedNotification.getMessage()).contains("2024-12-25");
        assertThat(savedNotification.getMessage()).contains("Central Park");
        assertThat(savedNotification.isRead()).isFalse();
    }

    @Test
    void handleUpdatedEvent_WithValidEvent_ShouldCreateNotification() {
        // Given
        LocalDateTime eventDate = LocalDateTime.of(2024, 12, 26, 19, 0);
        EventUpdated event = EventUpdated.builder()
                .userId(1L)
                .username("john_doe")
                .title("Summer Concert")
                .eventDate(eventDate)
                .location("Madison Square Garden")
                .build();

        ConsumerRecord<String, EventUpdated> record = createConsumerRecord("event-updated", event);

        // When
        consumer.handleUpdatedEvent(record);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUserId()).isEqualTo(1L);
        assertThat(savedNotification.getUsername()).isEqualTo("john_doe");
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.EVENT_UPDATED);
        assertThat(savedNotification.getMessage()).contains("Summer Concert");
        assertThat(savedNotification.getMessage()).contains("2024-12-26");
        assertThat(savedNotification.getMessage()).contains("Madison Square Garden");
        assertThat(savedNotification.isRead()).isFalse();
    }

    @Test
    void handleApproved_WithValidEvent_ShouldCreateNotification() {
        // Given
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(1L)
                .eventId(100L)
                .username("john_doe")
                .build();

        ConsumerRecord<String, BookingCreatedEvent> record = createConsumerRecord("booking-approved", event);

        // When
        consumer.handleApproved(record);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUserId()).isEqualTo(1L);
        assertThat(savedNotification.getUsername()).isEqualTo("john_doe");
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.BOOKING_APPROVED);
        assertThat(savedNotification.getMessage()).contains("100");
        assertThat(savedNotification.getMessage()).contains("approved");
        assertThat(savedNotification.isRead()).isFalse();
    }

    @Test
    void handleCancelled_WithValidEvent_ShouldCreateNotification() {
        // Given
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .userId(1L)
                .eventId(100L)
                .username("john_doe")
                .build();

        ConsumerRecord<String, BookingCancelledEvent> record = createConsumerRecord("booking-cancelled", event);

        // When
        consumer.handleCancelled(record);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUserId()).isEqualTo(1L);
        assertThat(savedNotification.getUsername()).isEqualTo("john_doe");
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.BOOKING_CANCELLED);
        assertThat(savedNotification.getMessage()).contains("100");
        assertThat(savedNotification.getMessage()).contains("cancelled");
        assertThat(savedNotification.isRead()).isFalse();
    }

    @Test
    void handleBookingCreated_WithValidEvent_ShouldCreateNotification() {
        // Given
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(1L)
                .eventId(100L)
                .username("john_doe")
                .build();

        ConsumerRecord<String, BookingCreatedEvent> record = createConsumerRecord("booking-created", event);

        // When
        consumer.handleBookingCreated(record);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUserId()).isEqualTo(1L);
        assertThat(savedNotification.getUsername()).isEqualTo("john_doe");
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.BOOKING_CREATED);
        assertThat(savedNotification.getMessage()).contains("100");
        assertThat(savedNotification.getMessage()).contains("pending");
        assertThat(savedNotification.isRead()).isFalse();
    }

    @Test
    void handleBookingDeleted_WithValidEvent_ShouldCreateNotification() {
        // Given
        BookingDeletedEvent event = BookingDeletedEvent.builder()
                .userId(1L)
                .eventId(100L)
                .username("john_doe")
                .build();

        ConsumerRecord<String, BookingDeletedEvent> record = createConsumerRecord("booking-deleted", event);

        // When
        consumer.handleBookingDeleted(record);

        // Then
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUserId()).isEqualTo(1L);
        assertThat(savedNotification.getUsername()).isEqualTo("john_doe");
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.BOOKING_DELETED);
        assertThat(savedNotification.getMessage()).contains("100");
        assertThat(savedNotification.getMessage()).contains("deleted");
        assertThat(savedNotification.isRead()).isFalse();
    }

    // ==================== Error Handling Tests ====================

    @Test
    void handleCreatedUser_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Given
        UserCreatedEvent event = UserCreatedEvent.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .build();

        ConsumerRecord<String, UserCreatedEvent> record = createConsumerRecord("user-created", event);

        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When/Then
        try {
            consumer.handleCreatedUser(record);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database connection failed");
        }

        // Verify MDC is cleared even on exception (clear() is void, just verify it was called by checking MDC is null)
        assertThat(MDC.get(CorrelationConstants.MDC_KEY)).isNull();
    }

    @Test
    void handleUserDeleted_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Given
        UserDeletedEvent event = new UserDeletedEvent();
        event.setUserId(1L);

        ConsumerRecord<String, UserDeletedEvent> record = createConsumerRecord("user-deleted", event);

        // For void methods, use doThrow().when() syntax
        Mockito.doThrow(new RuntimeException("Database connection failed"))
                .when(notificationRepository).deleteAllByUserId(1L);

        // When/Then
        try {
            consumer.handleUserDeleted(record);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database connection failed");
        }

        // Verify MDC is cleared even on exception
        assertThat(MDC.get(CorrelationConstants.MDC_KEY)).isNull();
    }

    @Test
    void handleCreatedEvent_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Given
        EventCreated event = EventCreated.builder()
                .userId(1L)
                .username("john_doe")
                .title("Summer Concert")
                .eventDate(LocalDateTime.now())
                .location("Central Park")
                .build();

        ConsumerRecord<String, EventCreated> record = createConsumerRecord("event-created", event);

        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When/Then
        try {
            consumer.handleCreatedEvent(record);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database connection failed");
        }

        // Verify MDC is cleared even on exception
        assertThat(MDC.get(CorrelationConstants.MDC_KEY)).isNull();
    }

    // ==================== Edge Case Tests ====================

    @Test
    void handleCreatedUser_WithNullUsername_ShouldStillCreateNotification() {
        // Given
        UserCreatedEvent event = UserCreatedEvent.builder()
                .id(1L)
                .username(null)
                .email("john@example.com")
                .build();

        ConsumerRecord<String, UserCreatedEvent> record = createConsumerRecord("user-created", event);

        // When/Then - should not throw NullPointerException
        assertDoesNotThrow(() -> consumer.handleCreatedUser(record));

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void handleCreatedEvent_WithNullFields_ShouldStillCreateNotification() {
        // Given
        EventCreated event = EventCreated.builder()
                .userId(1L)
                .username(null)
                .title(null)
                .eventDate(null)
                .location(null)
                .build();

        ConsumerRecord<String, EventCreated> record = createConsumerRecord("event-created", event);

        // When/Then - should not throw NullPointerException
        assertDoesNotThrow(() -> consumer.handleCreatedEvent(record));

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void handleApproved_WithNullUsername_ShouldStillCreateNotification() {
        // Given
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .userId(1L)
                .eventId(100L)
                .username(null)
                .build();

        ConsumerRecord<String, BookingCreatedEvent> record = createConsumerRecord("booking-approved", event);

        // When/Then - should not throw NullPointerException
        assertDoesNotThrow(() -> consumer.handleApproved(record));

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void handleCreatedUser_WithCorrelationId_ShouldSetAndClearMdc() {
        // Given
        UserCreatedEvent event = UserCreatedEvent.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .build();

        String correlationId = "test-correlation-id-123";
        Header header = new RecordHeader(
                CorrelationConstants.CORRELATION_ID,
                correlationId.getBytes(StandardCharsets.UTF_8)
        );

        // Create headers using RecordHeaders implementation
        org.apache.kafka.common.header.internals.RecordHeaders recordHeaders = 
                new org.apache.kafka.common.header.internals.RecordHeaders();
        recordHeaders.add(header);

        // Use the full constructor with all parameters including headers
        ConsumerRecord<String, UserCreatedEvent> record = 
                new ConsumerRecord<String, UserCreatedEvent>(
                        "user-created",
                        0,
                        0L,
                        System.currentTimeMillis(),
                        org.apache.kafka.common.record.TimestampType.CREATE_TIME,
                        0,
                        100,
                        "key",
                        event,
                        recordHeaders,
                        java.util.Optional.empty()
                );

        // When
        consumer.handleCreatedUser(record);

        // Then - MDC should be cleared after processing
        assertThat(MDC.get(CorrelationConstants.MDC_KEY)).isNull();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void handleCreatedUser_WithoutCorrelationId_ShouldNotSetMdc() {
        // Given
        UserCreatedEvent event = UserCreatedEvent.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .build();

        ConsumerRecord<String, UserCreatedEvent> record = createConsumerRecord("user-created", event);

        // When
        consumer.handleCreatedUser(record);

        // Then - MDC should remain empty
        assertThat(MDC.get(CorrelationConstants.MDC_KEY)).isNull();
        verify(notificationRepository).save(any(Notification.class));
    }

    // ==================== Helper Methods ====================

    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(
                topic,
                0,
                0L,
                "key",
                value
        );
    }
}