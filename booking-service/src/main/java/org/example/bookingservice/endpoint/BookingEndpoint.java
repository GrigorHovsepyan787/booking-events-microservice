package org.example.bookingservice.endpoint;

import lombok.RequiredArgsConstructor;
import org.example.bookingservice.dto.BookingDto;
import org.example.bookingservice.dto.request.BookingRequest;
import org.example.bookingservice.service.BookingService;
import org.example.bookingservice.service.security.JwtPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingEndpoint {
    private final BookingService bookingService;

    @GetMapping
    public List<BookingDto> getBookings(@AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        return bookingService.findAllByUserId(userId);
    }

    @GetMapping("/{id}")
    public BookingDto getBooking(@PathVariable Long id,
                                 @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        return bookingService.findById(id, userId);
    }

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(@RequestBody BookingRequest request,
                                                    @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        String username = principal.getUsername();
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request, userId, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id,
                                              @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        String username = principal.getUsername();
        bookingService.delete(id, userId, username);
        return ResponseEntity.noContent().build();
    }
}
