package org.example.bookingservice.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bookingservice.dto.response.BookingResponse;
import org.example.bookingservice.dto.request.BookingRequest;
import org.example.bookingservice.service.BookingService;
import org.example.securitycommon.principal.JwtPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
@Tag(name = "Bookings")
public class BookingEndpoint {
    private final BookingService bookingService;

    @Operation(summary = "Get Bookings of user")
    @GetMapping
    public Page<BookingResponse> getBookings(@AuthenticationPrincipal JwtPrincipal principal,
                                             @PageableDefault(size = 20) Pageable pageable) {
        Long userId = principal.getUserId();
        return bookingService.findAllByUserId(userId, pageable);
    }

    @Operation(summary = "Get booking by id")
    @GetMapping("/{id}")
    public BookingResponse getBooking(@PathVariable Long id,
                                      @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        return bookingService.findById(id, userId);
    }

    @Operation(summary = "Create Booking")
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request,
                                                         @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        String username = principal.getUsername();
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request, userId, username));
    }

    @Operation(summary = "Delete Booking")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id,
                                              @AuthenticationPrincipal JwtPrincipal principal) {
        Long userId = principal.getUserId();
        String username = principal.getUsername();
        bookingService.delete(id, userId, username);
        return ResponseEntity.noContent().build();
    }
}
