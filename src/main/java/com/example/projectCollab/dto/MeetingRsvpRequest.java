package com.example.projectCollab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRsvpRequest {

    @NotBlank(message = "RSVP response is required")
    private String response;
}
