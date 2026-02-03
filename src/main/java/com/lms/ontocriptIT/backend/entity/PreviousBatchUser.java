package com.lms.ontocriptIT.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "previous_batch_users") // New Table Name
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviousBatchUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // New ID for the archive table

    private Long originalUserId; // To keep track of what their ID used to be

    private String studentId;
    private String firstName;
    private String lastName;
    private String district;
    private String idNumber;
    private String email;
    private String phoneNumber1;
    private String phoneNumber2;

    // We store the password hash just in case, but usually not needed for archives
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime originalCreatedAt; // When they joined
    private LocalDateTime archivedAt; // When we moved them to this table

    @PrePersist
    protected void onArchive() {
        archivedAt = LocalDateTime.now();
    }
}