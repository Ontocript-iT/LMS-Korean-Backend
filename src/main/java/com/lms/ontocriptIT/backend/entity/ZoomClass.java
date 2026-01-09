package com.lms.ontocriptIT.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "zoom_classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoomClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String zoomLink;

    @Column(nullable = false)
    private String classDate;

    @Column(nullable = false)
    private String classTime;


    @Column(columnDefinition = "TEXT")
    private String note;

    private boolean isActive = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
