package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.dtos.VideoAccessDTO;
import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.services.centralServices.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class VideoController {

    private final VideoService videoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("groupName") String groupName,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal User admin) {

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Video file is required");
        }

        return videoService.uploadVideo(title, description, file, admin.getId(),groupName);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllVideos() {
        return videoService.getAllVideos();
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> getActiveVideos() {
        return videoService.getActiveVideos();
    }

    @GetMapping("/{videoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> getVideoById(@PathVariable Long videoId) {
        return videoService.getVideoById(videoId);
    }

    @DeleteMapping("/{videoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteVideo(
            @PathVariable Long videoId,
            @AuthenticationPrincipal User admin) {
        return videoService.deleteVideo(videoId, admin.getId());
    }

    @PostMapping("/grant-access")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> grantVideoAccess(
            @Valid @RequestBody VideoAccessDTO dto,
            @AuthenticationPrincipal User admin) {
        return videoService.grantVideoAccess(dto, admin.getId());
    }

    @PutMapping("/revoke-access/{accessId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeVideoAccess(
            @PathVariable Long accessId,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal User admin) {
        return videoService.revokeVideoAccess(accessId, admin.getId(), notes);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> getStudentVideos(@PathVariable Long studentId) {
        return videoService.getStudentVideos(studentId);
    }

    @GetMapping("/embed/{videoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> getSecureVideoEmbed(
            @PathVariable Long videoId,
            @AuthenticationPrincipal User student) {
        return videoService.getSecureVideoEmbed(videoId, student.getId());
    }

    @PostMapping("/record-attempt/{videoId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> recordVideoAttempt(
            @PathVariable Long videoId,
            @AuthenticationPrincipal User student) {
        return videoService.recordVideoAttempt(videoId, student.getId());
    }

    @GetMapping("/access/{videoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> getStudentVideoAccess(
            @PathVariable Long videoId,
            @AuthenticationPrincipal User student) {
        return videoService.getStudentVideoAccess(student.getId(), videoId);
    }

    @GetMapping("/check/{videoId}")
    public ResponseEntity<?> checkAccess(@PathVariable Long videoId, HttpServletRequest request, @AuthenticationPrincipal User user) {

        boolean canWatch = videoService.canWatch(user.getId(), videoId);

        if (!canWatch) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Watch limit exceeded (1.25x duration reached)."));
        }
        return ResponseEntity.ok(Map.of("status", "ALLOWED"));
    }

    @PostMapping("/heartbeat/{videoId}")
    public ResponseEntity<?> trackHeartbeat(@PathVariable Long videoId,@AuthenticationPrincipal User user) {
        try {

            int heartbeatInterval = 10;

            Map<String, Object> stats = videoService.trackWatchTime(user.getId(), videoId, heartbeatInterval);
            return ResponseEntity.ok(stats);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/setTotalWatchTimeTpZero/{videoId}")
    public ResponseEntity<?> setTotalWatchTimeTpZero(
            @PathVariable Long videoId,@AuthenticationPrincipal User user) {
        return videoService.setTotalWatchTimeToZero(videoId,user.getId());
    }
}
