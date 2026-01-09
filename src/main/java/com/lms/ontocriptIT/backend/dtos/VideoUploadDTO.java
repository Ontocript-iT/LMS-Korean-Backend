package com.lms.ontocriptIT.backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoUploadDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
}
