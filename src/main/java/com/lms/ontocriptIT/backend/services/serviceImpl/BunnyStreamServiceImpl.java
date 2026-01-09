package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.ontocriptIT.backend.services.centralServices.BunnyStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BunnyStreamServiceImpl implements BunnyStreamService {

    @Value("${bunny.stream.library-id}")
    private String libraryId;

    @Value("${bunny.stream.api-key}")
    private String apiKey;

    @Value("${bunny.stream.cdn-hostname}")
    private String cdnHostname;

    @Value("${bunny.stream.security-key}")
    private String securityKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, String> uploadVideoFile(String title, MultipartFile file) {
        try {
            // Step 1: Create video object in Bunny.net
            String videoId = createVideoObject(title);

            // Step 2: Upload the actual video file
            uploadVideoContent(videoId, file);

            // Step 3: Get video information including URLs
            Map<String, Object> videoInfo = getVideoInfo(videoId);

            // Extract URLs
            String playbackUrl = String.format(
                    "https://iframe.mediadelivery.net/embed/%s/%s",
                    libraryId,
                    videoId
            );

            String thumbnailUrl = String.format(
                    "https://vz-%s.b-cdn.net/%s/thumbnail.jpg",
                    libraryId,
                    videoId
            );

            Map<String, String> result = new HashMap<>();
            result.put("videoId", videoId);
            result.put("videoUrl", playbackUrl);
            result.put("thumbnailUrl", thumbnailUrl);
            result.put("status", "success");

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video to Bunny.net: " + e.getMessage(), e);
        }
    }

    /**
     * Step 1: Create video object in Bunny.net library
     */
    private String createVideoObject(String title) {
        try {
            String createUrl = "https://video.bunnycdn.com/library/" + libraryId + "/videos";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("AccessKey", apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("title", title);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    createUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                return response.getBody().get("guid").toString();
            } else {
                throw new RuntimeException("Failed to create video object: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error creating video object: " + e.getMessage(), e);
        }
    }

    /**
     * Step 2: Upload video file content
     */
    private void uploadVideoContent(String videoId, MultipartFile file) {
        try {
            String uploadUrl = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId;

            // Use Java 11+ HttpClient for better file upload handling
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("AccessKey", apiKey)
                    .header("Content-Type", "application/octet-stream")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new RuntimeException("Failed to upload video file: HTTP " + response.statusCode());
            }

            System.out.println("Video file uploaded successfully: " + videoId);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error uploading video file: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getVideoInfo(String videoId) {
        try {
            String infoUrl = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("AccessKey", apiKey);

            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    infoUrl,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get video info: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateSecureToken(String videoId, long expirationTime) {
        try {
            String tokenBase = securityKey + videoId + String.valueOf(expirationTime);

            Mac sha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    this.securityKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256.init(secretKey);

            byte[] hash = sha256.doFinal(tokenBase.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate secure token: " + e.getMessage(), e);
        }
    }

    @Override
    public String getSecureEmbedUrl(String videoId) {
        long expirationTime = System.currentTimeMillis() / 1000 + 3600; // 1 hour expiry
        String token = generateSecureToken(videoId, expirationTime);

        return String.format(
                "https://iframe.mediadelivery.net/embed/%s/%s?token=%s&expires=%d&controls=speed",
                libraryId,
                videoId,
                token,
                expirationTime
        );
    }

    @Override
    public void deleteVideo(String videoId) {
        try {
            String deleteUrl = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("AccessKey", apiKey);

            HttpEntity<?> request = new HttpEntity<>(headers);

            restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );

            System.out.println("Video deleted successfully: " + videoId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete video from Bunny.net: " + e.getMessage(), e);
        }
    }
}
