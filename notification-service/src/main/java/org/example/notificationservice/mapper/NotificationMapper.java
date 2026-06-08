package org.example.notificationservice.mapper;

import org.example.notificationservice.dto.NotificationDto;
import org.example.notificationservice.dto.SimpleNotificationDto;
import org.example.notificationservice.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationDto toDto(Notification notification);

    SimpleNotificationDto toSimpleDto(Notification notification);
}
