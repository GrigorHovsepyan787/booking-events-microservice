package org.example.eventservice.repository;

import org.example.eventservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByUserId(Long userId);

    @Modifying
    @Query("""
                update Event e
                set e.seatsAvailable = e.seatsAvailable - 1
                where e.id = :eventId
                  and e.seatsAvailable > 0
            """)
    int reserveSeat(Long eventId);

    @Modifying
    @Query("""
                update Event e
                set e.seatsAvailable = e.seatsAvailable + 1
                where e.id = :eventId
                  and e.seatsAvailable < e.capacity
            """)
    int cancelReservation(Long eventId);

    void deleteAllByUserId(Long userId);
}
