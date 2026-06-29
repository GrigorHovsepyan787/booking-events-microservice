package org.example.bookingservice.endpoint;

import org.example.bookingservice.dto.request.BookingRequest;
import org.example.bookingservice.dto.response.BookingResponse;
import org.example.bookingservice.entity.BookingStatus;
import org.example.bookingservice.service.BookingService;
import org.example.securitycommon.principal.JwtPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingEndpoint.class)
class BookingEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private org.example.securitycommon.parser.JwtParser jwtParser;

    private final Long TEST_USER_ID = 1L;
    private final String TEST_USERNAME = "testuser";
    private final Long TEST_BOOKING_ID = 100L;
    private final Long TEST_EVENT_ID = 42L;

    private RequestPostProcessor mockAuthentication() {
        JwtPrincipal principal = new JwtPrincipal(TEST_USER_ID, TEST_USERNAME);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return authentication(auth);
    }

    @Test
    @DisplayName("GET /api/bookings - Success")
    void getBookings_Success() throws Exception {
        BookingResponse bookingResponse = new BookingResponse(TEST_BOOKING_ID, TEST_EVENT_ID, BookingStatus.CONFIRMED);
        Page<BookingResponse> page = new PageImpl<>(List.of(bookingResponse), PageRequest.of(0, 20), 1);

        when(bookingService.findAllByUserId(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/bookings")
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(TEST_BOOKING_ID))
                .andExpect(jsonPath("$.content[0].eventId").value(TEST_EVENT_ID))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/bookings/{id} - Success")
    void getBooking_Success() throws Exception {
        BookingResponse bookingResponse = new BookingResponse(TEST_BOOKING_ID, TEST_EVENT_ID, BookingStatus.CONFIRMED);

        when(bookingService.findById(eq(TEST_BOOKING_ID), eq(TEST_USER_ID))).thenReturn(bookingResponse);

        mockMvc.perform(get("/api/bookings/{id}", TEST_BOOKING_ID)
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(TEST_BOOKING_ID))
                .andExpect(jsonPath("$.eventId").value(TEST_EVENT_ID))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/bookings - Success")
    void createBooking_Success() throws Exception {
        BookingRequest request = new BookingRequest(TEST_EVENT_ID);
        BookingResponse bookingResponse = new BookingResponse(TEST_BOOKING_ID, TEST_EVENT_ID, BookingStatus.PENDING);

        when(bookingService.create(any(BookingRequest.class), eq(TEST_USER_ID), eq(TEST_USERNAME))).thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings")
                        .with(mockAuthentication())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(TEST_BOOKING_ID))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("DELETE /api/bookings/{id} - Success")
    void deleteBooking_Success() throws Exception {
        doNothing().when(bookingService).delete(eq(TEST_BOOKING_ID), eq(TEST_USER_ID), eq(TEST_USERNAME));

        mockMvc.perform(delete("/api/bookings/{id}", TEST_BOOKING_ID)
                        .with(mockAuthentication())
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}