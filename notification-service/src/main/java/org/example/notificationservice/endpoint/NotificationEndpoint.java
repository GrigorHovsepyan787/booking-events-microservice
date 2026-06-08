package org.example.notificationservice.endpoint;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.dto.NotificationDto;
import org.example.notificationservice.dto.SimpleNotificationDto;
import org.example.notificationservice.service.NotificationService;
import org.example.notificationservice.service.security.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationEndpoint {
    private final NotificationService notificationService;

    @GetMapping
    public List<SimpleNotificationDto> getNotifications(@AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        return notificationService.findAllByUserId(userId);
    }

    @PatchMapping("/{id}")
    public NotificationDto readNotification(@PathVariable Long id,
                                            @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        return notificationService.markAsRead(id, userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id,
                                                   @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        notificationService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
