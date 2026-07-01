package org.example.eventservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EventPageResponse{
    private List<EventResponse> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
}