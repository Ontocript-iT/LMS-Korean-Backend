package com.lms.ontocriptIT.backend.dtos;

import com.lms.ontocriptIT.backend.entity.VideoStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String groupName;
    private String bunnyVideoId;
    private String videoUrl;
    private String thumbnailUrl;
    private YearMonth uploadMonth;
    private int duration;
    private Long fileSizeBytes;
    private String fileSizeMB;
    private String originalFileName;
    private VideoStatus status;
    private String uploadedByName;
    private boolean isActive;
    private LocalDateTime createdAt;
}
