package org.example.eventservice.mapper;

import org.example.eventservice.dto.request.CreateEventRequest;
import org.example.eventservice.dto.response.EventResponse;
import org.example.eventservice.entity.Event;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {
    EventResponse toDto(Event event);
    Event toEntity(CreateEventRequest dto);
}
