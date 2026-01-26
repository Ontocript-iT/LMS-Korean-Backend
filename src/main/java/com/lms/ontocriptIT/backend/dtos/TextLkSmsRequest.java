package com.lms.ontocriptIT.backend.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TextLkSmsRequest {

    @JsonProperty("recipient")
    private String recipient; // E.g., "94771234567"

    @JsonProperty("sender_id")
    private String senderId;

    @JsonProperty("type")
    private String type; // Usually "plain"

    @JsonProperty("message")
    private String message;

    // Optional: schedule_time
}