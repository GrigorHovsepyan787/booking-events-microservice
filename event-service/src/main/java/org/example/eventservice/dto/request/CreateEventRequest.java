package org.example.eventservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class CreateEventRequest {
    @NotBlank(message = "Title is required!")
    @Size(min = 5, max = 50, message = "Title must contain from 5 to 50 characters")
    private String title;

    @NotBlank(message = "Description is required!")
    @Size(min = 15, max = 100, message = "Description must contain from 15 to 100 characters")
    private String description;

    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    @NotBlank(message = "Location is required!")
    private String location;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}
