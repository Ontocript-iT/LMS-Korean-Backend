package com.lms.ontocriptIT.backend.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_access", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "video_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private boolean hasAccess;

    @Column(nullable = false)
    private int maxAttempts; // Default: 2

    @Column(nullable = false)
    private int attemptsUsed;

    private LocalDateTime lastWatchedAt;

    private LocalDateTime accessGrantedDate;

    private LocalDateTime accessRevokedDate;

    private long totalSecondsWatched = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        maxAttempts = 2; // Default
        attemptsUsed = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isLimitExceeded() {
        return totalSecondsWatched >= video.getMaxWatchTime();
    }
}

