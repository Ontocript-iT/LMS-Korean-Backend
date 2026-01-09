package com.lms.ontocriptIT.backend.dtos;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryDTO {
    private Long id;
    private Long userId;
    private String studentId;
    private String studentName;
    private LocalDateTime loginTime;
    private String ipAddress;
    private String deviceType;
    private String browser;
    private String operatingSystem;
    private boolean loginSuccessful;
    private String failureReason;
}
