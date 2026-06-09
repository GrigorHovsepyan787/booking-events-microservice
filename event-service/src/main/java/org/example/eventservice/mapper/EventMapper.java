package org.example.eventservice.mapper;

import org.example.eventservice.dto.CreateEventDto;
import org.example.eventservice.dto.EventDto;
import org.example.eventservice.entity.Event;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {
    EventDto toDto(Event event);
    Event toEntity(CreateEventDto dto);
}
