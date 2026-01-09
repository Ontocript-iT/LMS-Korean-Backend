package com.lms.ontocriptIT.backend.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationDTO {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "ID number is required")
    @Pattern(regexp = "^[0-9]{9}[Vv]$|^[0-9]{12}$", message = "Invalid Sri Lankan ID number")
    private String idNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number 1 is required")
    @Pattern(regexp = "^(\\+94|0)?[0-9]{9}$", message = "Invalid Sri Lankan phone number")
    private String phoneNumber1;

    @NotBlank(message = "Phone number 2 is required")
    @Pattern(regexp = "^(\\+94|0)?[0-9]{9}$", message = "Invalid Sri Lankan phone number")
    private String phoneNumber2;
}
