package org.example.notificationservice.entity;

import lombok.Getter;

@Getter
public enum NotificationType {
    USER_CREATED("Welcome to service!"),
    EVENT_CREATED("Event created!"),
    EVENT_DELETED("Event deleted!"),
    EVENT_UPDATED("Event updated!"),
    BOOKING_APPROVED("Booking approved1"),
    BOOKING_CANCELLED("Booking cancelled!"),
    BOOKING_CREATED("Booking created!"),
    BOOKING_DELETED("Booking deleted");

    private final String title;

    NotificationType(String title) {
        this.title = title;
    }
}
