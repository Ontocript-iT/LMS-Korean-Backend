package com.lms.ontocriptIT.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    private String ipAddress;

    private String deviceType; // Mobile, Desktop, Tablet

    private String browser;

    private String operatingSystem;

    private String userAgent;

    private String location; // City, Country

    @Column(nullable = false)
    private boolean loginSuccessful;

    private String failureReason;


    @Column(name = "device_fingerprint", nullable = false, unique = true)
    private String deviceFingerprint; // Unique device identifier

    @Column(name = "is_current_session", nullable = false, columnDefinition = "boolean default false")
    private boolean isCurrentSession; // Track active session


    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
