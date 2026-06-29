package org.example.notificationservice.endpoint;

import org.example.notificationservice.kafka.consumer.NotificationEventConsumer;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;
import org.example.notificationservice.dto.response.NotificationResponse;
import org.example.notificationservice.dto.response.SimpleNotificationResponse;
import org.example.notificationservice.exception.NotificationNotFoundException;
import org.example.notificationservice.service.NotificationService;
import org.springframework.context.annotation.Import;
import org.example.securitycommon.principal.JwtPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationEndpoint.class)
@Import(org.example.notificationservice.exception.GlobalExceptionHandler.class)
class NotificationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private NotificationEventConsumer notificationEventConsumer;

    @MockitoBean
    private org.example.securitycommon.parser.JwtParser jwtParser;

    private final String TEST_NOTIFICATION_ID = "notif-123";

    private RequestPostProcessor mockAuthentication() {
        JwtPrincipal principal = new JwtPrincipal(1L, "testuser");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return authentication(auth);
    }

    @Test
    @DisplayName("GET /api/notifications - Happy Path - Should return paginated notifications")
    void getNotifications_HappyPath_ReturnsOkWithNotifications() throws Exception {
        SimpleNotificationResponse notif1 = new SimpleNotificationResponse("1", "Welcome");
        SimpleNotificationResponse notif2 = new SimpleNotificationResponse("2", "New Event");
        Pageable pageable = PageRequest.of(0, 10);
        Page<SimpleNotificationResponse> page = new PageImpl<>(List.of(notif1, notif2), pageable, 2);

        when(notificationService.findAllByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/notifications")
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value("1"))
                .andExpect(jsonPath("$.content[0].title").value("Welcome"));
    }

    @Test
    @DisplayName("GET /api/notifications - Error Mapping - Should return 404 when notification not found")
    void getNotifications_NotFound_Returns404() throws Exception {
        when(notificationService.findAllByUserId(eq(1L), any(Pageable.class)))
                .thenThrow(new NotificationNotFoundException("No notifications found for user"));

        mockMvc.perform(get("/api/notifications")
                        .with(mockAuthentication()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No notifications found for user"))
                .andExpect(jsonPath("$.details").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id} - Happy Path - Should mark notification as read")
    void readNotification_HappyPath_ReturnsOkWithNotification() throws Exception {
        NotificationResponse response = new NotificationResponse(
                TEST_NOTIFICATION_ID,
                "john_doe",
                "Event Reminder",
                "Your event starts in 1 hour",
                LocalDateTime.now()
        );

        when(notificationService.markAsRead(eq(TEST_NOTIFICATION_ID), eq(1L))).thenReturn(response);

        mockMvc.perform(patch("/api/notifications/{id}", TEST_NOTIFICATION_ID)
                        .with(mockAuthentication())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(TEST_NOTIFICATION_ID))
                .andExpect(jsonPath("$.title").value("Event Reminder"))
                .andExpect(jsonPath("$.message").value("Your event starts in 1 hour"));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id} - Error Mapping - Should return 404 when notification not found")
    void readNotification_NotFound_Returns404() throws Exception {
        when(notificationService.markAsRead(eq(TEST_NOTIFICATION_ID), eq(1L)))
                .thenThrow(new NotificationNotFoundException("Notification not found with id: " + TEST_NOTIFICATION_ID));

        mockMvc.perform(patch("/api/notifications/{id}", TEST_NOTIFICATION_ID)
                        .with(mockAuthentication())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Notification not found with id: " + TEST_NOTIFICATION_ID))
                .andExpect(jsonPath("$.details").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id} - Error Mapping - Should return 403 on AccessDeniedException")
    void readNotification_AccessDenied_Returns403() throws Exception {
        when(notificationService.markAsRead(eq(TEST_NOTIFICATION_ID), eq(1L)))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(patch("/api/notifications/{id}", TEST_NOTIFICATION_ID)
                        .with(mockAuthentication())
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.details").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} - Happy Path - Should delete notification and return 204")
    void deleteNotification_HappyPath_ReturnsNoContent() throws Exception {
        doNothing().when(notificationService).delete(eq(TEST_NOTIFICATION_ID), eq(1L));

        mockMvc.perform(delete("/api/notifications/{id}", TEST_NOTIFICATION_ID)
                        .with(mockAuthentication())
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} - Error Mapping - Should return 404 when notification not found")
    void deleteNotification_NotFound_Returns404() throws Exception {
        doThrow(new NotificationNotFoundException("Notification not found with id: " + TEST_NOTIFICATION_ID))
                .when(notificationService).delete(eq(TEST_NOTIFICATION_ID), eq(1L));

        mockMvc.perform(delete("/api/notifications/{id}", TEST_NOTIFICATION_ID)
                        .with(mockAuthentication())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Notification not found with id: " + TEST_NOTIFICATION_ID))
                .andExpect(jsonPath("$.details").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} - Error Mapping - Should return 403 on AccessDeniedException")
    void deleteNotification_AccessDenied_Returns403() throws Exception {
        doThrow(new AccessDeniedException("Access denied"))
                .when(notificationService).delete(eq(TEST_NOTIFICATION_ID), eq(1L));

        mockMvc.perform(delete("/api/notifications/{id}", TEST_NOTIFICATION_ID)
                        .with(mockAuthentication())
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.details").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }
}