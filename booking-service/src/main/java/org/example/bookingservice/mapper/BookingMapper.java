package org.example.bookingservice.mapper;

import org.example.bookingservice.dto.response.BookingResponse;
import org.example.bookingservice.dto.request.BookingRequest;
import org.example.bookingservice.entity.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    BookingResponse toDto(Booking booking);
    Booking toEntity(BookingRequest request);
}
