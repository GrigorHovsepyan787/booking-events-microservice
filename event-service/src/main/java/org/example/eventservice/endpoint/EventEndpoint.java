package org.example.eventservice.endpoint;

import lombok.RequiredArgsConstructor;
import org.example.eventservice.dto.CreateEventDto;
import org.example.eventservice.dto.EventDto;
import org.example.eventservice.service.EventService;
import org.example.eventservice.service.security.JwtPrincipal;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventEndpoint {
    private final EventService eventService;

    @GetMapping
    public List<EventDto> getEvents() {
        return eventService.getEvents();
    }

    @GetMapping("/my")
    public List<EventDto> getMyEvents(@AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        return eventService.getUserEvents(userId);
    }

    @GetMapping("/{id}")
    public EventDto findEvent(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @PostMapping
    public ResponseEntity<EventDto> create(@RequestBody CreateEventDto dto,
                                           @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        String username = principal.getUsername();
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(dto, userId, username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDto> update(@RequestBody CreateEventDto dto,
                                           @PathVariable Long id,
                                           @AuthenticationPrincipal JwtPrincipal principal) {
        String username = principal.getUsername();
        Long userId = principal.getUserId();
        return ResponseEntity.ok().body(eventService.update(dto, userId, id, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal JwtPrincipal principal) {
        String username = principal.getUsername();
        Long userId = principal.getUserId();
        eventService.delete(id, userId, username);
        return ResponseEntity.noContent().build();
    }
}
