package com.lms.ontocriptIT.backend.dtos;

import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassAccessResponseDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String classLink;
    private String className;
    private String classDate;
    private boolean hasAccess;
    private LocalDateTime accessGrantedDate;
    private LocalDateTime accessRevokedDate;
    private String grantedByName;
    private String revokedByName;
    private String notes;
    private String classTime;
}
