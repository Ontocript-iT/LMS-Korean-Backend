package com.lms.ontocriptIT.backend.dtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecureVideoEmbedDTO {
    private String embedUrl;
    private String token;
    private Long expiresAt;
    private int remainingAttempts;
    private String videoTitle;
    private String thumbnailUrl;
}
