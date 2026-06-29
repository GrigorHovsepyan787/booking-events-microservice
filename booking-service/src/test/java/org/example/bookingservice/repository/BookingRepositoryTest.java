package org.example.bookingservice.repository;

import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Booking booking1;
    private Booking booking2;
    private Booking booking3;

    @BeforeEach
    void setUp() {
        booking1 = new Booking(null, 100L, 1L, BookingStatus.PENDING, null, null);
        booking2 = new Booking(null, 100L, 2L, BookingStatus.CONFIRMED, null, null);
        booking3 = new Booking(null, 200L, 1L, BookingStatus.PENDING, null, null);

        booking1 = bookingRepository.save(booking1);
        booking2 = bookingRepository.save(booking2);
        booking3 = bookingRepository.save(booking3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAllByUserId_ShouldReturnPagedBookingsForUser() {
        Page<Booking> result = bookingRepository.findAllByUserId(100L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(b -> b.getUserId().equals(100L)));
    }

    @Test
    void findByUserIdAndEventId_ShouldReturnBooking_WhenExists() {
        Optional<Booking> result = bookingRepository.findByUserIdAndEventId(100L, 1L);

        assertTrue(result.isPresent());
        assertEquals(booking1.getId(), result.get().getId());
    }

    @Test
    void findByUserIdAndEventId_ShouldReturnEmpty_WhenNotExists() {
        Optional<Booking> result = bookingRepository.findByUserIdAndEventId(100L, 99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void updateStatusByEventId_ShouldUpdateStatusForMatchingBookings() {
        bookingRepository.updateStatusByEventId(1L, BookingStatus.CANCELLED);

        entityManager.clear();

        Optional<Booking> updatedBooking1 = bookingRepository.findById(booking1.getId());
        Optional<Booking> updatedBooking3 = bookingRepository.findById(booking3.getId());
        Optional<Booking> updatedBooking2 = bookingRepository.findById(booking2.getId());

        assertTrue(updatedBooking1.isPresent());
        assertEquals(BookingStatus.CANCELLED, updatedBooking1.get().getStatus());

        assertTrue(updatedBooking3.isPresent());
        assertEquals(BookingStatus.CANCELLED, updatedBooking3.get().getStatus());

        assertTrue(updatedBooking2.isPresent());
        assertEquals(BookingStatus.CONFIRMED, updatedBooking2.get().getStatus());
    }

    @Test
    void deleteAllByUserId_ShouldDeleteOnlyTargetUserBookings() {
        bookingRepository.deleteAllByUserId(100L);

        entityManager.clear();

        Page<Booking> user1Bookings = bookingRepository.findAllByUserId(100L, PageRequest.of(0, 10));
        Page<Booking> user2Bookings = bookingRepository.findAllByUserId(200L, PageRequest.of(0, 10));

        assertTrue(user1Bookings.isEmpty());
        assertEquals(1, user2Bookings.getTotalElements());
    }
}