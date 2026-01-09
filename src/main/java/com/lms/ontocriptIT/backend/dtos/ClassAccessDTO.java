package com.lms.ontocriptIT.backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassAccessDTO {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotBlank(message = "Class link is required")
    private String classLink;

    @NotBlank(message = "Class name is required")
    private String className;

    @NotNull(message = "Access status is required")
    private boolean hasAccess;

    private Long zoomClassId;

    private String notes;
}
