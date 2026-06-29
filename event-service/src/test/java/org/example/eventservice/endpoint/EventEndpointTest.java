package org.example.eventservice.endpoint;

import org.example.eventservice.dto.request.CreateEventRequest;
import org.example.eventservice.dto.response.EventResponse;
import org.example.eventservice.service.EventService;
import org.example.securitycommon.parser.JwtParser;
import org.example.securitycommon.principal.JwtPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
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

import java.time.LocalDateTime;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventEndpoint.class)
class EventEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private JwtParser jwtParser;

    private final Long TEST_USER_ID = 1L;
    private final String TEST_USERNAME = "testuser";
    private final Long TEST_EVENT_ID = 42L;

    private RequestPostProcessor mockAuthentication() {
        JwtPrincipal principal = new JwtPrincipal(TEST_USER_ID, TEST_USERNAME);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return authentication(auth);
    }

    private EventResponse createMockEventResponse(Long id, String title) {
        return new EventResponse(
                id,
                title,
                "Description for " + title,
                LocalDateTime.now().plusDays(2),
                "Main Hall",
                100,
                100
        );
    }

    @Test
    @DisplayName("GET /api/events - Happy Path - Should return paginated public events")
    void getEvents_HappyPath_ReturnsOkWithPage() throws Exception {
        EventResponse event1 = createMockEventResponse(1L, "Java Conference");
        EventResponse event2 = createMockEventResponse(2L, "Spring Boot Workshop");
        Page<EventResponse> page = new PageImpl<>(List.of(event1, event2), PageRequest.of(0, 20), 2);

        when(eventService.getEvents(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/events")
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Java Conference"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].title").value("Spring Boot Workshop"));
    }

    @Test
    @DisplayName("GET /api/events/my - Happy Path - Should return paginated user's events")
    void getMyEvents_HappyPath_ReturnsOkWithPage() throws Exception {
        EventResponse myEvent = createMockEventResponse(10L, "My Personal Event");
        Page<EventResponse> page = new PageImpl<>(List.of(myEvent), PageRequest.of(0, 10), 1);

        when(eventService.getUserEvents(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/events/my")
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].title").value("My Personal Event"));
    }

    @Test
    @DisplayName("GET /api/events/{id} - Happy Path - Should return event by id")
    void findEvent_HappyPath_ReturnsEvent() throws Exception {
        EventResponse response = createMockEventResponse(TEST_EVENT_ID, "Concert");

        when(eventService.getEvent(eq(TEST_EVENT_ID))).thenReturn(response);

        mockMvc.perform(get("/api/events/{id}", TEST_EVENT_ID)
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(TEST_EVENT_ID))
                .andExpect(jsonPath("$.title").value("Concert"))
                .andExpect(jsonPath("$.location").value("Main Hall"));
    }

    @Test
    @DisplayName("POST /api/events - Happy Path - Should create event and return 201 Created")
    void create_HappyPath_ReturnsCreated() throws Exception {
        CreateEventRequest requestDto = new CreateEventRequest(
                "New Spectacular Event",
                "Amazing description",
                LocalDateTime.now().plusDays(5),
                "Online",
                500
        );
        EventResponse responseDto = createMockEventResponse(100L, "New Spectacular Event");

        when(eventService.create(any(CreateEventRequest.class), eq(TEST_USER_ID), eq(TEST_USERNAME)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/events")
                        .with(mockAuthentication())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("New Spectacular Event"));
    }

    @Test
    @DisplayName("PUT /api/events/{id} - Happy Path - Should update event and return 200 OK")
    void update_HappyPath_ReturnsOk() throws Exception {
        CreateEventRequest requestDto = new CreateEventRequest(
                "Updated Event Title",
                "Updated description",
                LocalDateTime.now().plusDays(3),
                "New Location",
                150
        );
        EventResponse responseDto = createMockEventResponse(TEST_EVENT_ID, "Updated Event Title");
        responseDto.setLocation("New Location");

        when(eventService.update(any(CreateEventRequest.class), eq(TEST_USER_ID), eq(TEST_EVENT_ID), eq(TEST_USERNAME)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/events/{id}", TEST_EVENT_ID)
                        .with(mockAuthentication())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(TEST_EVENT_ID))
                .andExpect(jsonPath("$.title").value("Updated Event Title"))
                .andExpect(jsonPath("$.location").value("New Location"));
    }

    @Test
    @DisplayName("DELETE /api/events/{id} - Happy Path - Should delete event and return 204 No Content")
    void delete_HappyPath_ReturnsNoContent() throws Exception {
        doNothing().when(eventService).delete(eq(TEST_EVENT_ID), eq(TEST_USER_ID), eq(TEST_USERNAME));

        mockMvc.perform(delete("/api/events/{id}", TEST_EVENT_ID)
                        .with(mockAuthentication())
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/events - Validation Failure - Should return 400 Bad Request when title is blank")
    void create_InvalidRequest_Returns400BadRequest() throws Exception {
        // Создаем некорректный DTO (например, с пустым заголовок, если стоит @NotBlank)
        CreateEventRequest invalidRequest = new CreateEventRequest(
                "", // Пустой title спровоцирует ошибку валидации @Valid
                "Some description",
                LocalDateTime.now().plusDays(1),
                "Location",
                10
        );

        mockMvc.perform(post("/api/events")
                        .with(mockAuthentication())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}