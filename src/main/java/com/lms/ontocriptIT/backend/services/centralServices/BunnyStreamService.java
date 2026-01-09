package com.lms.ontocriptIT.backend.services.centralServices;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface BunnyStreamService {
    Map<String, String> uploadVideoFile(String title, MultipartFile file);
    String generateSecureToken(String videoId, long expirationTime);
    String getSecureEmbedUrl(String videoId);
    void deleteVideo(String videoId);
    Map<String, Object> getVideoInfo(String videoId);
}