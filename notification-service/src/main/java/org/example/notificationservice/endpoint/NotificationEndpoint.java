package org.example.notificationservice.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.notificationservice.dto.response.NotificationResponse;
import org.example.notificationservice.dto.response.SimpleNotificationResponse;
import org.example.notificationservice.service.NotificationService;
import org.example.securitycommon.principal.JwtPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationEndpoint {
    private final NotificationService notificationService;

    @Operation(summary = "Get all notifications of user")
    @GetMapping
    public Page<SimpleNotificationResponse> getNotifications(@AuthenticationPrincipal JwtPrincipal principal,
                                                             @PageableDefault Pageable pageable) {
        Long userId = principal.getUserId();
        return notificationService.findAllByUserId(userId, pageable);
    }

    @Operation(summary = "Read the notification")
    @PatchMapping("/{id}")
    public NotificationResponse readNotification(@PathVariable String id,
                                                 @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        return notificationService.markAsRead(id, userId);
    }

    @Operation(summary = "Delete the notification")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id,
                                                   @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        notificationService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
