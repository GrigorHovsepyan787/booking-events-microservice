package org.example.bookingservice.mapper;

import org.example.bookingservice.dto.BookingDto;
import org.example.bookingservice.dto.request.BookingRequest;
import org.example.bookingservice.entity.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    BookingDto toDto(Booking booking);
    Booking toEntity(BookingRequest request);
}
