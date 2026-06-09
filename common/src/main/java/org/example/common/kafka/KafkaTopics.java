package org.example.common.kafka;

public final class KafkaTopics {
    private KafkaTopics() {}
    public static final String USER_CREATED_TOPIC = "user.created";
    public static final String USER_DELETED_TOPIC = "user.deleted";
    public static final String EVENT_CREATED_TOPIC = "event.created";
    public static final String EVENT_UPDATED_TOPIC = "event.updated";
    public static final String EVENT_DELETED_TOPIC = "event.deleted";
    public static final String BOOKING_CREATED_TOPIC = "booking.created";
    public static final String BOOKING_DELETED_TOPIC = "booking.deleted";
    public static final String BOOKING_APPROVED_TOPIC = "booking.approved";
    public static final String BOOKING_CANCELLED_TOPIC = "booking.cancelled";
}
