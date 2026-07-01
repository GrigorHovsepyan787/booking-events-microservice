package org.example.eventservice.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventservice.dto.response.EventPageResponse;
import org.example.eventservice.dto.response.EventResponse;
import org.example.eventservice.dto.request.CreateEventRequest;
import org.example.eventservice.service.EventService;
import org.example.securitycommon.principal.JwtPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
@Tag(name = "Events")
public class EventEndpoint {
    private final EventService eventService;

    @Operation(summary = "Get all events page")
    @GetMapping
    public EventPageResponse getEvents(@PageableDefault(size = 20) Pageable pageable) {
        return eventService.getEvents(pageable);
    }

    @Operation(summary = "Get users events pages")
    @GetMapping("/my")
    public EventPageResponse getMyEvents(@AuthenticationPrincipal JwtPrincipal principal,
                                           @PageableDefault Pageable pageable) {
        Long userId = principal.getUserId();
        return eventService.getUserEvents(userId, pageable);
    }

    @Operation(summary = "Get event by id")
    @GetMapping("/{id}")
    public EventResponse findEvent(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @Operation(summary = "Create new event")
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest dto,
                                                @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        String username = principal.getUsername();
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(dto, userId, username));
    }

    @Operation(summary = "Update the event")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(@Valid @RequestBody CreateEventRequest dto,
                                                @PathVariable Long id,
                                                @AuthenticationPrincipal JwtPrincipal principal) {
        String username = principal.getUsername();
        Long userId = principal.getUserId();
        return ResponseEntity.ok().body(eventService.update(dto, userId, id, username));
    }

    @Operation(summary = "Delete the event")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal JwtPrincipal principal) {
        String username = principal.getUsername();
        Long userId = principal.getUserId();
        eventService.delete(id, userId, username);
        return ResponseEntity.noContent().build();
    }
}
