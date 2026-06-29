package org.example.notificationservice.repository;

import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.entity.NotificationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private final Long USER_ID_1 = 100L;
    private final Long USER_ID_2 = 200L;

    @BeforeEach
    void setUp() {
        Notification n1 = Notification.builder()
                .id("1")
                .userId(USER_ID_1)
                .username("john_doe")
                .type(NotificationType.USER_CREATED)
                .message("Welcome john_doe!")
                .read(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Notification n2 = Notification.builder()
                .id("2")
                .userId(USER_ID_1)
                .username("john_doe")
                .type(NotificationType.BOOKING_CREATED)
                .message("Booking #456 created")
                .read(true)
                .createdAt(LocalDateTime.now())
                .build();

        Notification n3 = Notification.builder()
                .id("3")
                .userId(USER_ID_1)
                .username("john_doe")
                .type(NotificationType.BOOKING_APPROVED)
                .message("Booking #456 approved")
                .read(false)
                .createdAt(LocalDateTime.now().plusDays(1))
                .build();

        Notification n4 = Notification.builder()
                .id("4")
                .userId(USER_ID_2)
                .username("alice_smith")
                .type(NotificationType.EVENT_CREATED)
                .message("New event available")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.saveAll(List.of(n1, n2, n3, n4));
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
    }

    @Test
    void findByUserId_ShouldReturnPagedNotificationsForSpecificUser() {
        Pageable pageable = PageRequest.of(0, 2);

        Page<Notification> result = notificationRepository.findByUserId(USER_ID_1, pageable);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements(), "USER_1 should have only 3 notifications in DB");
        assertEquals(2, result.getContent().size(), "First page must contain 2 elements");

        assertTrue(result.getContent().stream().allMatch(n -> n.getUserId().equals(USER_ID_1)));

        assertEquals("john_doe", result.getContent().get(0).getUsername());
    }

    @Test
    void findByUserId_ShouldReturnEmptyPageIfUserHasNoNotifications() {
        Long nonExistingUserId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<Notification> result = notificationRepository.findByUserId(nonExistingUserId, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void deleteAllByUserId_ShouldDeleteOnlyTargetUserNotifications() {
        notificationRepository.deleteAllByUserId(USER_ID_1);

        Page<Notification> remainingUser1 = notificationRepository.findByUserId(USER_ID_1, Pageable.unpaged());
        assertTrue(remainingUser1.isEmpty());

        Page<Notification> remainingUser2 = notificationRepository.findByUserId(USER_ID_2, Pageable.unpaged());
        assertEquals(1, remainingUser2.getTotalElements());

        Notification remainingNotification = remainingUser2.getContent().get(0);
        assertEquals("alice_smith", remainingNotification.getUsername());
        assertEquals(NotificationType.EVENT_CREATED, remainingNotification.getType());
    }
}
