package org.example.bookingservice.repository;

import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.BookingStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends CrudRepository<Booking, Long> {
    void deleteAllByUserId(Long userId);

    List<Booking> findAllByUserId(Long userId);

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
