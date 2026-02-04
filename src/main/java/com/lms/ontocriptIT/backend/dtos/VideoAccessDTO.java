package com.lms.ontocriptIT.backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAccessDTO {

    private Long studentId;

    @NotNull(message = "Video ID is required")
    private Long videoId;

    @NotNull(message = "Access status is required")
    private boolean hasAccess;

    private Integer maxAttempts; // Optional, defaults to 2

    private String notes;

    private Boolean isBulkAccess;
}
