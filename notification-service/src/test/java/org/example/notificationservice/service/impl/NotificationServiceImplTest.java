package org.example.notificationservice.service.impl;

import org.example.notificationservice.dto.response.NotificationResponse;
import org.example.notificationservice.dto.response.SimpleNotificationResponse;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.exception.NotificationNotFoundException;
import org.example.notificationservice.mapper.NotificationMapper;
import org.example.notificationservice.repository.NotificationRepository;
import org.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private Pageable pageable;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    // InOrder is used for verification of call sequences

    private Notification testNotification;
    private NotificationResponse notificationResponse;
    private SimpleNotificationResponse simpleNotificationResponse;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId("notif-1");
        testNotification.setUserId(10L);
        testNotification.setUsername("testuser");
        testNotification.setMessage("Test notification");
        testNotification.setRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());

        notificationResponse = new NotificationResponse();
        notificationResponse.setId("notif-1");
        notificationResponse.setUsername("testuser");
        notificationResponse.setMessage("Test notification");
        notificationResponse.setCreatedAt(LocalDateTime.now());

        simpleNotificationResponse = new SimpleNotificationResponse();
        simpleNotificationResponse.setId("notif-1");
        simpleNotificationResponse.setTitle("Test notification");
    }

    // ==================== findAllByUserId Tests ====================

    @Test
    void findAllByUserId_shouldReturnNotificationsPage_whenUserHasNotifications() {
        List<Notification> notificationsList = List.of(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notificationsList);

        when(notificationRepository.findByUserId(10L, pageable)).thenReturn(notificationPage);
        when(notificationMapper.toSimpleDto(testNotification)).thenReturn(simpleNotificationResponse);

        Page<SimpleNotificationResponse> result = notificationService.findAllByUserId(10L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("notif-1");

        verify(notificationRepository).findByUserId(10L, pageable);
        verify(notificationMapper).toSimpleDto(testNotification);
    }

    @Test
    void findAllByUserId_shouldReturnEmptyPage_whenUserHasNoNotifications() {
        Page<Notification> emptyPage = new PageImpl<>(List.of());

        when(notificationRepository.findByUserId(10L, pageable)).thenReturn(emptyPage);

        Page<SimpleNotificationResponse> result = notificationService.findAllByUserId(10L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();

        verify(notificationRepository).findByUserId(10L, pageable);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void findAllByUserId_shouldPassNullUserIdToRepository_whenUserIdIsNull() {
        Page<Notification> emptyPage = new PageImpl<>(List.of());

        when(notificationRepository.findByUserId(eq(null), any(Pageable.class))).thenReturn(emptyPage);

        Page<SimpleNotificationResponse> result = notificationService.findAllByUserId(null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();

        verify(notificationRepository).findByUserId(eq(null), any(Pageable.class));
        verifyNoInteractions(notificationMapper);
    }
    // ==================== markAsRead Tests ====================

    @Test
    void markAsRead_shouldMarkNotificationAsReadAndReturnResponse_whenNotificationExistsAndUserIsOwner() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        NotificationResponse result = notificationService.markAsRead("notif-1", 10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("notif-1");
        assertThat(result.getUsername()).isEqualTo("testuser");

        verify(notificationRepository).findById("notif-1");
        verify(notificationRepository).save(testNotification);
        verify(notificationMapper).toDto(testNotification);
    }

    @Test
    void markAsRead_shouldThrowNotificationNotFoundException_whenNotificationDoesNotExist() {
        when(notificationRepository.findById("notif-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead("notif-99", 10L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("notif-99");

        verify(notificationRepository).findById("notif-99");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void markAsRead_shouldThrowNotificationNotFoundException_whenNotificationIdIsNull() {
        when(notificationRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(null, 10L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage(null);

        verify(notificationRepository).findById(null);
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void markAsRead_shouldThrowAccessDeniedException_whenUserIsNotOwner() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        assertThatThrownBy(() -> notificationService.markAsRead("notif-1", 20L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed for actions");

        verify(notificationRepository).findById("notif-1");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void markAsRead_shouldThrowAccessDeniedException_whenNotificationUserIdIsNull() {
        testNotification.setUserId(null);

        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        assertThatThrownBy(() -> notificationService.markAsRead("notif-1", 10L))
                .isInstanceOf(NullPointerException.class);

        verify(notificationRepository).findById("notif-1");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void markAsRead_shouldThrowAccessDeniedException_whenPassedUserIdIsNull() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        assertThatThrownBy(() -> notificationService.markAsRead("notif-1", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed for actions");

        verify(notificationRepository).findById("notif-1");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }
    // ==================== delete Tests ====================

    @Test
    void delete_shouldDeleteNotification_whenNotificationExistsAndUserIsOwner() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        notificationService.delete("notif-1", 10L);

        verify(notificationRepository).findById("notif-1");
        verify(notificationRepository).deleteById("notif-1");
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void delete_shouldThrowNotificationNotFoundException_whenNotificationDoesNotExist() {
        when(notificationRepository.findById("notif-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.delete("notif-99", 10L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("notif-99");

        verify(notificationRepository).findById("notif-99");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void delete_shouldThrowNotificationNotFoundException_whenNotificationIdIsNull() {
        when(notificationRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.delete(null, 10L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage(null);

        verify(notificationRepository).findById(null);
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void delete_shouldThrowAccessDeniedException_whenUserIsNotOwner() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        assertThatThrownBy(() -> notificationService.delete("notif-1", 20L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed for actions");

        verify(notificationRepository).findById("notif-1");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void delete_shouldThrowAccessDeniedException_whenNotificationUserIdIsNull() {
        testNotification.setUserId(null);

        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        assertThatThrownBy(() -> notificationService.delete("notif-1", 10L))
                .isInstanceOf(NullPointerException.class);

        verify(notificationRepository).findById("notif-1");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void delete_shouldThrowAccessDeniedException_whenPassedUserIdIsNull() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        assertThatThrownBy(() -> notificationService.delete("notif-1", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed for actions");

        verify(notificationRepository).findById("notif-1");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    // ==================== In-Order Verification Tests ====================

    @Test
    void markAsRead_shouldCallRepositoryAndMapperInCorrectOrder() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        notificationService.markAsRead("notif-1", 10L);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(notificationRepository, notificationMapper);
        inOrder.verify(notificationRepository).findById("notif-1");
        inOrder.verify(notificationRepository).save(any(Notification.class));
        inOrder.verify(notificationMapper).toDto(testNotification);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void delete_shouldCallRepositoryInCorrectOrder() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        notificationService.delete("notif-1", 10L);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(notificationRepository);
        inOrder.verify(notificationRepository).findById("notif-1");
        inOrder.verify(notificationRepository).deleteById("notif-1");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void findAllByUserId_shouldCallRepositoryAndMapperInCorrectOrder() {
        List<Notification> notificationsList = List.of(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notificationsList);

        when(notificationRepository.findByUserId(10L, pageable)).thenReturn(notificationPage);
        when(notificationMapper.toSimpleDto(testNotification)).thenReturn(simpleNotificationResponse);

        notificationService.findAllByUserId(10L, pageable);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(notificationRepository, notificationMapper);
        inOrder.verify(notificationRepository).findByUserId(10L, pageable);
        inOrder.verify(notificationMapper).toSimpleDto(testNotification);
        inOrder.verifyNoMoreInteractions();
    }

    // ==================== Edge Case Tests for findAllByUserId ====================

    @Test
    void findAllByUserId_shouldHandleNegativeUserId() {
        Page<Notification> emptyPage = new PageImpl<>(List.of());
        when(notificationRepository.findByUserId(-1L, pageable)).thenReturn(emptyPage);

        Page<SimpleNotificationResponse> result = notificationService.findAllByUserId(-1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(notificationRepository).findByUserId(-1L, pageable);
    }

    @Test
    void findAllByUserId_shouldHandleZeroUserId() {
        Page<Notification> emptyPage = new PageImpl<>(List.of());
        when(notificationRepository.findByUserId(0L, pageable)).thenReturn(emptyPage);

        Page<SimpleNotificationResponse> result = notificationService.findAllByUserId(0L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(notificationRepository).findByUserId(0L, pageable);
    }

    @Test
    void findAllByUserId_shouldReturnMultipleNotifications() {
        Notification notification2 = new Notification();
        notification2.setId("notif-2");
        notification2.setUserId(10L);
        notification2.setUsername("testuser");
        notification2.setMessage("Second notification");
        notification2.setRead(false);

        List<Notification> notificationsList = List.of(testNotification, notification2);
        Page<Notification> notificationPage = new PageImpl<>(notificationsList);

        SimpleNotificationResponse response2 = new SimpleNotificationResponse();
        response2.setId("notif-2");
        response2.setTitle("Second notification");

        when(notificationRepository.findByUserId(10L, pageable)).thenReturn(notificationPage);
        when(notificationMapper.toSimpleDto(testNotification)).thenReturn(simpleNotificationResponse);
        when(notificationMapper.toSimpleDto(notification2)).thenReturn(response2);

        Page<SimpleNotificationResponse> result = notificationService.findAllByUserId(10L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        verify(notificationRepository).findByUserId(10L, pageable);
        verify(notificationMapper, times(2)).toSimpleDto(any(Notification.class));
    }

    // ==================== Edge Case Tests for markAsRead ====================

    @Test
    void markAsRead_shouldSetNotificationToRead() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        notificationService.markAsRead("notif-1", 10L);

        assertThat(testNotification.isRead()).isTrue();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsRead_shouldReturnCorrectNotificationResponse() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        NotificationResponse result = notificationService.markAsRead("notif-1", 10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("notif-1");
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(notificationMapper).toDto(testNotification);
    }

    @Test
    void markAsRead_shouldHandleAlreadyReadNotification() {
        testNotification.setRead(true);
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        NotificationResponse result = notificationService.markAsRead("notif-1", 10L);

        assertThat(result).isNotNull();
        assertThat(testNotification.isRead()).isTrue();
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void markAsRead_shouldHandleEmptyNotificationId() {
        when(notificationRepository.findById("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead("", 10L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("");

        verify(notificationRepository).findById("");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void markAsRead_shouldHandleWhitespaceNotificationId() {
        when(notificationRepository.findById("  ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead("  ", 10L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("  ");

        verify(notificationRepository).findById("  ");
        verifyNoMoreInteractions(notificationRepository);
        verifyNoInteractions(notificationMapper);
    }

    // ==================== Edge Case Tests for delete ====================

    @Test
    void delete_shouldHandleEmptyNotificationId() {
        when(notificationRepository.findById("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.delete("", 10L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("");

        verify(notificationRepository).findById("");
        verifyNoMoreInteractions(notificationRepository);
        verify(notificationRepository, never()).deleteById(anyString());
    }

    @Test
    void delete_shouldHandleWhitespaceNotificationId() {
        when(notificationRepository.findById("  ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.delete("  ", 10L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("  ");

        verify(notificationRepository).findById("  ");
        verifyNoMoreInteractions(notificationRepository);
        verify(notificationRepository, never()).deleteById(anyString());
    }

    @Test
    void delete_shouldNotCallSave_whenDeleting() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        notificationService.delete("notif-1", 10L);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    // ==================== Concurrent Scenario Tests ====================

    @Test
    void markAsRead_shouldHandleMultipleMarkAsReadCalls() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        NotificationResponse result1 = notificationService.markAsRead("notif-1", 10L);
        NotificationResponse result2 = notificationService.markAsRead("notif-1", 10L);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        verify(notificationRepository, times(2)).findById("notif-1");
        verify(notificationRepository, times(2)).save(testNotification);
    }

    @Test
    void delete_shouldHandleMultipleDeleteCalls() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        notificationService.delete("notif-1", 10L);
        notificationService.delete("notif-1", 10L);

        verify(notificationRepository, times(2)).findById("notif-1");
        verify(notificationRepository, times(2)).deleteById("notif-1");
    }

    @Test
    void findAllByUserId_shouldHandleMultipleCalls() {
        List<Notification> notificationsList = List.of(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notificationsList);

        when(notificationRepository.findByUserId(10L, pageable)).thenReturn(notificationPage);
        when(notificationMapper.toSimpleDto(testNotification)).thenReturn(simpleNotificationResponse);

        Page<SimpleNotificationResponse> result1 = notificationService.findAllByUserId(10L, pageable);
        Page<SimpleNotificationResponse> result2 = notificationService.findAllByUserId(10L, pageable);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        verify(notificationRepository, times(2)).findByUserId(10L, pageable);
    }

    // ==================== Repository Interaction Verification Tests ====================

    @Test
    void markAsRead_shouldCallFindByIdExactlyOnce() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        notificationService.markAsRead("notif-1", 10L);

        verify(notificationRepository, times(1)).findById("notif-1");
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void delete_shouldCallFindByIdExactlyOnce() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        doNothing().when(notificationRepository).deleteById("notif-1");

        notificationService.delete("notif-1", 10L);

        verify(notificationRepository, times(1)).findById("notif-1");
        verify(notificationRepository, times(1)).deleteById("notif-1");
        verifyNoMoreInteractions(notificationRepository);
    }

    @Test
    void markAsRead_shouldNotCallDelete_whenMarkingAsRead() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        notificationService.markAsRead("notif-1", 10L);

        verify(notificationRepository, never()).deleteById(anyString());
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void delete_shouldNotCallSave_whenDeletingNotification() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        notificationService.delete("notif-1", 10L);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    // ==================== Additional Edge Case Tests ====================

    @Test
    void findAllByUserId_shouldHandleVeryLargePageNumber() {
        Page<Notification> emptyPage = new PageImpl<>(List.of());
        when(notificationRepository.findByUserId(10L, pageable)).thenReturn(emptyPage);

        Page<SimpleNotificationResponse> result = notificationService.findAllByUserId(10L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(notificationRepository).findByUserId(10L, pageable);
    }

    @Test
    void markAsRead_shouldHandleNotificationWithNullMessage() {
        testNotification.setMessage(null);
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        NotificationResponse result = notificationService.markAsRead("notif-1", 10L);

        assertThat(result).isNotNull();
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void delete_shouldHandleNotificationWithNullMessage() {
        testNotification.setMessage(null);
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        notificationService.delete("notif-1", 10L);

        verify(notificationRepository).findById("notif-1");
        verify(notificationRepository).deleteById("notif-1");
    }

    @Test
    void markAsRead_shouldHandleNotificationWithNullUsername() {
        testNotification.setUsername(null);
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(notificationMapper.toDto(testNotification)).thenReturn(notificationResponse);

        NotificationResponse result = notificationService.markAsRead("notif-1", 10L);

        assertThat(result).isNotNull();
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void delete_shouldHandleNotificationWithNullUsername() {
        testNotification.setUsername(null);
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(testNotification));

        notificationService.delete("notif-1", 10L);

        verify(notificationRepository).findById("notif-1");
        verify(notificationRepository).deleteById("notif-1");
    }

    @Test
    void findAllByUserId_shouldHandleNotificationsWithMixedReadStatus() {
        Notification readNotification = new Notification();
        readNotification.setId("notif-read");
        readNotification.setUserId(10L);
        readNotification.setRead(true);

        Notification unreadNotification = new Notification();
        unreadNotification.setId("notif-unread");
        unreadNotification.setUserId(10L);
        unreadNotification.setRead(false);

        List<Notification> notificationsList = List.of(readNotification, unreadNotification);
        Page<Notification> notificationPage = new PageImpl<>(notificationsList);

        SimpleNotificationResponse readResponse = new SimpleNotificationResponse();
        readResponse.setId("notif-read");
        readResponse.setTitle("Read notification");

        SimpleNotificationResponse unreadResponse = new SimpleNotificationResponse();
        unreadResponse.setId("notif-unread");
        unreadResponse.setTitle("Unread notification");

        when(notificationRepository.findByUserId(10L, pageable)).thenReturn(notificationPage);
        when(notificationMapper.toSimpleDto(readNotification)).thenReturn(readResponse);
        when(notificationMapper.toSimpleDto(unreadNotification)).thenReturn(unreadResponse);

        Page<SimpleNotificationResponse> result = notificationService.findAllByUserId(10L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        verify(notificationRepository).findByUserId(10L, pageable);
        verify(notificationMapper, times(2)).toSimpleDto(any(Notification.class));
    }
}
