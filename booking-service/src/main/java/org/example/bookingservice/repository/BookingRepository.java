package org.example.bookingservice.repository;

import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends CrudRepository<Booking, Long> {
    void deleteAllByUserId(Long userId);

    Page<Booking> findAllByUserId(Long userId, Pageable pageable);

    Optional<Booking> findByUserIdAndEventId(Long userId, Long eventId);

    @Modifying
    @Query("""
                update Booking b
                set b.status = :status
                where b.eventId = :eventId
            """)
    void updateStatusByEventId(
            @Param("eventId") Long eventId,
            @Param("status") BookingStatus status
    );
}
