package org.example.eventservice.repository;

import org.example.eventservice.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findByUserId(Long userId, Pageable pageable);

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
