package org.example.eventservice.repository;

import org.example.eventservice.entity.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        event1 = new Event(
                null,
                "Title 1",
                "Description 1",
                LocalDateTime.now().plusDays(1),
                "Location 1",
                100,
                50,
                1L,
                null,
                null,
                null
        );

        event2 = new Event(
                null,
                "Title 2",
                "Description 2",
                LocalDateTime.now().plusDays(2),
                "Location 2",
                50,
                0,
                1L,
                null,
                null,
                null
        );

        event1 = eventRepository.save(event1);
        event2 = eventRepository.save(event2);

        Event event3 = new Event(
                null,
                "Title 3",
                "Description 3",
                LocalDateTime.now().plusDays(3),
                "Location 3",
                10,
                10,
                2L,
                null,
                null,
                null
        );
        eventRepository.save(event3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByUserId_ShouldReturnPagedEvents() {
        Page<Event> result = eventRepository.findByUserId(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void reserveSeat_ShouldDecrementSeatsAvailable_WhenSeatsAreAvailable() {
        int updatedRows = eventRepository.reserveSeat(event1.getId());

        entityManager.clear();
        Optional<Event> updatedEvent = eventRepository.findById(event1.getId());

        assertEquals(1, updatedRows);
        assertTrue(updatedEvent.isPresent());
        assertEquals(49, updatedEvent.get().getSeatsAvailable());
    }

    @Test
    void reserveSeat_ShouldNotDecrementSeatsAvailable_WhenNoSeatsAvailable() {
        int updatedRows = eventRepository.reserveSeat(event2.getId());

        entityManager.clear();
        Optional<Event> updatedEvent = eventRepository.findById(event2.getId());

        assertEquals(0, updatedRows);
        assertTrue(updatedEvent.isPresent());
        assertEquals(0, updatedEvent.get().getSeatsAvailable());
    }

    @Test
    void cancelReservation_ShouldIncrementSeatsAvailable_WhenSeatsLessThanCapacity() {
        int updatedRows = eventRepository.cancelReservation(event1.getId());

        entityManager.clear();
        Optional<Event> updatedEvent = eventRepository.findById(event1.getId());

        assertEquals(1, updatedRows);
        assertTrue(updatedEvent.isPresent());
        assertEquals(51, updatedEvent.get().getSeatsAvailable());
    }

    @Test
    void cancelReservation_ShouldNotIncrementSeatsAvailable_WhenSeatsEqualToCapacity() {
        Event eventAtCapacity = new Event(
                null,
                "Full",
                "Desc",
                LocalDateTime.now().plusDays(1),
                "Loc",
                10,
                10,
                1L,
                null,
                null,
                null
        );
        eventAtCapacity = eventRepository.save(eventAtCapacity);
        entityManager.flush();
        entityManager.clear();

        int updatedRows = eventRepository.cancelReservation(eventAtCapacity.getId());

        entityManager.clear();
        Optional<Event> updatedEvent = eventRepository.findById(eventAtCapacity.getId());

        assertEquals(0, updatedRows);
        assertTrue(updatedEvent.isPresent());
        assertEquals(10, updatedEvent.get().getSeatsAvailable());
    }

    @Test
    void deleteAllByUserId_ShouldDeleteOnlyUserEvents() {
        eventRepository.deleteAllByUserId(1L);

        entityManager.clear();
        Page<Event> user1Events = eventRepository.findByUserId(1L, PageRequest.of(0, 10));
        Page<Event> user2Events = eventRepository.findByUserId(2L, PageRequest.of(0, 10));

        assertTrue(user1Events.isEmpty());
        assertEquals(1, user2Events.getTotalElements());
    }
}
