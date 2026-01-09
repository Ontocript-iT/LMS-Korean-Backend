package com.lms.ontocriptIT.backend.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "Username is required")
    private String username; // Can be studentId, email, or phone number

    @NotBlank(message = "Password is required")
    private String password;
}