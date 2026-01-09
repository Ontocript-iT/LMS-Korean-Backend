package com.lms.ontocriptIT.backend.auth;

import com.lms.ontocriptIT.backend.entity.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false, unique = true)
    private String idNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber1;

    @Column(nullable = false)
    private String phoneNumber2;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private Long reviewedBy;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        status = RequestStatus.PENDING;
    }
}
