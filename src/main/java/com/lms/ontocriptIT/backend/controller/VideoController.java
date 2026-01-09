package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.dtos.VideoAccessDTO;
import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.services.centralServices.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal User admin) {

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Video file is required");
        }

        return videoService.uploadVideo(title, description, file, admin.getId());
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
}
