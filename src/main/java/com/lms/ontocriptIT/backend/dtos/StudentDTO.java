package com.lms.ontocriptIT.backend.dtos;

import java.time.LocalDateTime;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDTO {
    private Long id;
    private String studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String district;
    private String phoneNumber1;
    private String phoneNumber2;
    private String status;

    private String groupName;
    private LocalDateTime createdAt;
    private int paymentCount;
    private int classAccessCount;

}