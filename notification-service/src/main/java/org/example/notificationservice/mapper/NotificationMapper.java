package org.example.notificationservice.mapper;

import org.example.notificationservice.dto.response.NotificationResponse;
import org.example.notificationservice.dto.response.SimpleNotificationResponse;
import org.example.notificationservice.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toDto(Notification notification);

    SimpleNotificationResponse toSimpleDto(Notification notification);
}
