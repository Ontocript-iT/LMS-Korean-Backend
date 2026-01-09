package com.lms.ontocriptIT.backend.auth;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutRequestDTO {
    private String deviceFingerprint;
    private boolean logoutAllDevices;
}