package org.example.common.kafka;

public final class KafkaTopics {
    private KafkaTopics() {}
    public static final String USER_CREATED_TOPIC = "user.v1.created";
    public static final String USER_DELETED_TOPIC = "user.v1.deleted";
    public static final String EVENT_CREATED_TOPIC = "event.v1.created";
    public static final String EVENT_UPDATED_TOPIC = "event.v1.updated";
    public static final String EVENT_DELETED_TOPIC = "event.v1.deleted";
    public static final String BOOKING_CREATED_TOPIC = "booking.v1.created";
    public static final String BOOKING_DELETED_TOPIC = "booking.v1.deleted";
    public static final String BOOKING_APPROVED_TOPIC = "booking.v1.approved";
    public static final String BOOKING_CANCELLED_TOPIC = "booking.v1.cancelled";
}
