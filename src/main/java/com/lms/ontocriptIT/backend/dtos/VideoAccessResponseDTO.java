package com.lms.ontocriptIT.backend.dtos;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAccessResponseDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long videoId;
    private String videoUrl;
    private String videoTitle;
    private boolean hasAccess;
    private int maxAttempts;
    private int attemptsUsed;
    private int remainingAttempts;
    private LocalDateTime lastWatchedAt;
    private LocalDateTime accessGrantedDate;
    private String grantedByName;
    private String notes;
}

