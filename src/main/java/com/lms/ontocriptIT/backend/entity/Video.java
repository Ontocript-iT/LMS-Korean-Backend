package com.lms.ontocriptIT.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Entity
@Table(name = "videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false, unique = true)
    private String bunnyVideoId;

    @Column(nullable = false)
    private String bunnyLibraryId;

    @Column(nullable = false)  // ADD THIS FIELD
    private String videoUrl;

    private String thumbnailUrl;

    @Column(nullable = false)
    private int maxWatchTime;

    @Column(nullable = false)
    private YearMonth uploadMonth;

    @Column(nullable = false)
    private int duration;

    private Long fileSizeBytes;  // ADD THIS FIELD

    private String originalFileName;  // ADD THIS FIELD

    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    private boolean isActive;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL)
    private List<VideoAccess> videoAccesses;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;

        this.maxWatchTime = this.duration + (this.duration / 4);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        this.maxWatchTime = this.duration + (this.duration / 4);
    }

}
