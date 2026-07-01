package org.example.notificationservice.mapper;

import org.example.notificationservice.dto.response.NotificationResponse;
import org.example.notificationservice.dto.response.SimpleNotificationResponse;
import org.example.notificationservice.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "title", source = "type.title")
    NotificationResponse toDto(Notification notification);

    @Mapping(target = "title", source = "type.title")
    SimpleNotificationResponse toSimpleDto(Notification notification);
}
