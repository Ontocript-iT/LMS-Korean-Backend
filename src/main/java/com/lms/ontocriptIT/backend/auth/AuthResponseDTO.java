package com.lms.ontocriptIT.backend.auth;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private String token;
    private String studentId;
    private String email;
    private String role;
    private boolean isTemporaryPassword;
    private String message;
}