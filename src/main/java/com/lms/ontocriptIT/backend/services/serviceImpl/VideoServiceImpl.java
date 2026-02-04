package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.googlecode.mp4parser.DataSource;
import com.lms.ontocriptIT.backend.dtos.*;
import com.lms.ontocriptIT.backend.entity.*;
import com.lms.ontocriptIT.backend.repository.*;
import com.lms.ontocriptIT.backend.services.centralServices.BunnyStreamService;
import com.lms.ontocriptIT.backend.services.centralServices.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final VideoAccessRepository videoAccessRepository;
    private final VideoWatchLogRepository videoWatchLogRepository;
    private final UserRepository userRepository;
    private final BunnyStreamService bunnyStreamService;

    private final PaymentRepository paymentRepository;

    @Value("${bunny.stream.max-videos-per-month}")
    private int maxVideosPerMonth;

    @Value("${bunny.stream.library-id}")
    private String libraryId;

    @Override
    @Transactional
    public ResponseEntity<?> uploadVideo(String title, String description, MultipartFile file, Long adminId) {
        File tempFile = null;
        try {
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            YearMonth currentMonth = YearMonth.now();

            long videoCount = videoRepository.countActiveVideosByMonth(currentMonth);
            if (videoCount >= maxVideosPerMonth) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Monthly video upload limit reached. Maximum " + maxVideosPerMonth + " videos per month.");
                response.put("currentCount", videoCount);
                response.put("maxAllowed", maxVideosPerMonth);
                response.put("month", currentMonth.toString());
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (file.isEmpty()) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Video file is required");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Invalid file type. Only video files are allowed.");
                response.put("receivedType", contentType);
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            long maxFileSize = 5L * 1024 * 1024 * 1024; // 5 GB in bytes

            if (file.getSize() > maxFileSize) {
                double fileSizeMB = file.getSize() / (1024.0 * 1024.0);
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "File size exceeds maximum limit of 5GB");
                response.put("fileSize", String.format("%.2f MB", fileSizeMB));
                response.put("maxAllowed", "5120 MB"); // 5GB in MB
                response.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());

                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
            }

            tempFile = File.createTempFile("video_upload_temp", ".mp4");

            Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            long durationInSeconds = 0;

            try (IsoFile isoFile = new IsoFile(tempFile.getAbsolutePath())) {
                MovieBox movieBox = isoFile.getMovieBox();
                long duration = movieBox.getMovieHeaderBox().getDuration();
                long timescale = movieBox.getMovieHeaderBox().getTimescale();

                durationInSeconds = duration / timescale;
            }

            System.out.println("Video Duration: " + durationInSeconds + " seconds");
            
            Map<String, String> uploadResult = bunnyStreamService.uploadVideoFile(title, file);

            String bunnyVideoId = uploadResult.get("videoId");
            String videoUrl = uploadResult.get("videoUrl");
            String thumbnailUrl = uploadResult.get("thumbnailUrl");

            // Save video metadata to database
            Video video = Video.builder()
                    .title(title)
                    .description(description)
                    .bunnyVideoId(bunnyVideoId)
                    .bunnyLibraryId(libraryId)
                    .videoUrl(videoUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .uploadMonth(currentMonth)
                    .duration((int) durationInSeconds)
                    .fileSizeBytes(file.getSize())
                    .originalFileName(file.getOriginalFilename())
                    .status(VideoStatus.PROCESSING)
                    .uploadedBy(admin)
                    .isActive(true)
                    .build();

            Video savedVideo = videoRepository.save(video);
            VideoResponseDTO videoDTO = convertToResponseDTO(savedVideo);

            HashMap<String, Object> response = new HashMap<>();
            response.put("video", videoDTO);
            response.put("bunnyVideoId", bunnyVideoId);
            response.put("uploadedBy", admin.getFirstName() + " " + admin.getLastName());
            response.put("message", "Video uploaded successfully and is being processed");
            response.put("status", HttpStatus.CREATED.value());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to upload video: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public boolean canWatch(Long userId, Long videoId) {
        VideoAccess access = videoAccessRepository.findByStudentIdAndVideoId(userId, videoId)
                .orElse(null);

        if (access == null) return true;

        return !access.isLimitExceeded();
    }

    @Transactional
    public Map<String, Object> trackWatchTime(Long userId, Long videoId, int secondsToAdd) {
        VideoAccess access = videoAccessRepository.findByStudentIdAndVideoId(userId, videoId)
                .orElseThrow(() -> new RuntimeException("Access record not found"));

        if (access.isLimitExceeded()) {
            throw new RuntimeException("Watch limit exceeded");
        }

        // Add the time (e.g., +10 seconds)
        access.setTotalSecondsWatched(access.getTotalSecondsWatched() + secondsToAdd);
        videoAccessRepository.save(access);

        Map<String, Object> response = new HashMap<>();
        response.put("totalWatched", access.getTotalSecondsWatched());
        response.put("maxLimit", access.getVideo().getMaxWatchTime());
        response.put("remaining", Math.max(0, access.getVideo().getMaxWatchTime() - access.getTotalSecondsWatched()));

        return response;
    }

    @Override
    public ResponseEntity<?> getAllVideos() {
        try {
            List<Video> videos = videoRepository.findAll();
            List<VideoResponseDTO> responseDTOs = videos.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", responseDTOs);
            response.put("count", responseDTOs.size());
            response.put("message", "All videos retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve videos: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getActiveVideos() {
        try {
            List<Video> videos = videoRepository.findAllActiveVideosOrderByCreatedDesc();
            List<VideoResponseDTO> responseDTOs = videos.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", responseDTOs);
            response.put("count", responseDTOs.size());
            response.put("message", "Active videos retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve active videos: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getVideoById(Long videoId) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            VideoResponseDTO videoDTO = convertToResponseDTO(video);

            HashMap<String, Object> response = new HashMap<>();
            response.put("video", videoDTO);
            response.put("message", "Video retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("videoId", videoId);
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve video: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteVideo(Long videoId, Long adminId) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            String videoTitle = video.getTitle();
            String bunnyVideoId = video.getBunnyVideoId();

            videoWatchLogRepository.deleteByVideoId(videoId);
            videoAccessRepository.deleteByVideoId(videoId);

            // Delete from Bunny.net
            try {
                bunnyStreamService.deleteVideo(video.getBunnyVideoId());
            } catch (Exception e) {
//                System.err.println("Failed to delete from Bunny.net: " + e.getMessage());
            }
            videoRepository.delete(video);

            HashMap<String, Object> response = new HashMap<>();
            response.put("videoId", videoId);
            response.put("videoTitle", videoTitle);
            response.put("bunnyVideoId", bunnyVideoId);
            response.put("message", "Video deleted successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("videoId", videoId);
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to delete video: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> grantVideoAccess(VideoAccessDTO dto, Long adminId) {
        try {
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            Video video = videoRepository.findById(dto.getVideoId())
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            if (Boolean.TRUE.equals(dto.getIsBulkAccess())) {
                return handleBulkAccess(dto, video, admin);
            } else {
                return handleSingleAccess(dto, video, admin);
            }

        } catch (RuntimeException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse("Failed to process request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private ResponseEntity<?> handleBulkAccess(VideoAccessDTO dto, Video video, User admin) {
        YearMonth currentMonth = YearMonth.now();

        List<User> eligibleStudents = paymentRepository.findStudentsByMonthAndStatus(
                currentMonth, PaymentStatus.VERIFIED
        );

        if (eligibleStudents.isEmpty()) {
            throw new RuntimeException("No students found with completed payments for " + currentMonth);
        }

        int successCount = 0;

        for (User student : eligibleStudents) {
            saveOrUpdateAccess(student, video, admin, dto);
            successCount++;
        }

        HashMap<String, Object> response = new HashMap<>();
        response.put("message", "Bulk access granted successfully");
        response.put("videoId", video.getId());
        response.put("videoTitle", video.getTitle());
        response.put("studentsAffected", successCount);
        response.put("month", currentMonth.toString());
        response.put("grantedBy", admin.getFirstName());
        response.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleSingleAccess(VideoAccessDTO dto, Video video, User admin) {
        User student = userRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        VideoAccess savedAccess = saveOrUpdateAccess(student, video, admin, dto);

        HashMap<String, Object> response = new HashMap<>();
        response.put("accessId", savedAccess.getId());
        response.put("studentId", student.getId());
        response.put("studentName", student.getFirstName() + " " + student.getLastName());
        response.put("videoId", video.getId());
        response.put("hasAccess", savedAccess.isHasAccess());
        response.put("message", "Video access updated successfully");
        response.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    // Common logic to Create or Update Access
    private VideoAccess saveOrUpdateAccess(User student, Video video, User admin, VideoAccessDTO dto) {
        VideoAccess access = videoAccessRepository
                .findByStudentAndVideo(student, video)
                .orElse(null);

        if (access == null) {
            // Create New
            access = VideoAccess.builder()
                    .student(student)
                    .video(video)
                    .attemptsUsed(0)
                    .build();
        }

        // Update fields (for both new and existing)
        access.setHasAccess(dto.isHasAccess());
        access.setMaxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : 2);
        access.setNotes(dto.getNotes());

        if (dto.isHasAccess()) {
            access.setAccessGrantedDate(LocalDateTime.now());
            access.setGrantedBy(admin);
            access.setAccessRevokedDate(null);
            access.setRevokedBy(null);
            // Reset attempts on new grant? (Optional based on requirements)
            // access.setAttemptsUsed(0);
        }

        return videoAccessRepository.save(access);
    }

    private ResponseEntity<HashMap<String, Object>> createErrorResponse(String message, HttpStatus status) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("status", status.value());
        return ResponseEntity.status(status).body(response);
    }

    @Override
    @Transactional
    public ResponseEntity<?> revokeVideoAccess(Long accessId, Long adminId, String notes) {
        try {
            VideoAccess videoAccess = videoAccessRepository.findById(accessId)
                    .orElseThrow(() -> new RuntimeException("Video access not found"));

            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            videoAccess.setHasAccess(false);
            videoAccess.setAccessRevokedDate(LocalDateTime.now());
            videoAccess.setRevokedBy(admin);
            videoAccess.setNotes(notes);

            VideoAccess savedAccess = videoAccessRepository.save(videoAccess);

            HashMap<String, Object> response = new HashMap<>();
            response.put("accessId", savedAccess.getId());
            response.put("studentId", savedAccess.getStudent().getId());
            response.put("studentName", savedAccess.getStudent().getFirstName() + " " + savedAccess.getStudent().getLastName());
            response.put("videoTitle", savedAccess.getVideo().getTitle());
            response.put("hasAccess", false);
            response.put("revokedBy", admin.getFirstName() + " " + admin.getLastName());
            response.put("revokedDate", savedAccess.getAccessRevokedDate());
            response.put("notes", notes);
            response.put("message", "Video access revoked successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to revoke video access: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getStudentVideos(Long studentId) {
        try {
            List<VideoAccess> accesses = videoAccessRepository.findAvailableVideosForStudentDesc(studentId);

            List<VideoAccessResponseDTO> responseDTOs = accesses.stream()
                    .map(this::convertToAccessResponseDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", responseDTOs);
            response.put("count", responseDTOs.size());
            response.put("studentId", studentId);
            response.put("message", "Student videos retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve student videos: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getSecureVideoEmbed(Long videoId, Long studentId) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            VideoAccess access = videoAccessRepository.findByStudentIdAndVideoId(studentId, videoId)
                    .orElseThrow(() -> new RuntimeException("No access to this video"));

            // Check access and attempts
            if (!access.isHasAccess() || access.getAttemptsUsed() >= access.getMaxAttempts()) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Access denied or attempts exhausted");
                response.put("hasAccess", access.isHasAccess());
                response.put("attemptsUsed", access.getAttemptsUsed());
                response.put("maxAttempts", access.getMaxAttempts());
                response.put("status", HttpStatus.FORBIDDEN.value());

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Generate secure embed URL
            String embedUrl = bunnyStreamService.getSecureEmbedUrl(video.getBunnyVideoId());
            long expirationTime = System.currentTimeMillis() / 1000 + 3600;
            String token = bunnyStreamService.generateSecureToken(video.getBunnyVideoId(), expirationTime);

            HashMap<String, Object> response = new HashMap<>();
            response.put("embedUrl", embedUrl);
            response.put("token", token);
            response.put("expiresAt", expirationTime);
            response.put("remainingAttempts", access.getMaxAttempts() - access.getAttemptsUsed());
            response.put("videoTitle", video.getTitle());
            response.put("thumbnailUrl", video.getThumbnailUrl());
            response.put("duration", video.getDuration());
            response.put("message", "Secure video embed URL generated successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to generate secure embed URL: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> recordVideoAttempt(Long videoId, Long studentId) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            VideoAccess access = videoAccessRepository.findByStudentIdAndVideoId(studentId, videoId)
                    .orElseThrow(() -> new RuntimeException("No access to this video"));

            // Check if attempts remaining
            if (access.getAttemptsUsed() >= access.getMaxAttempts()) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "No attempts remaining");
                response.put("attemptsUsed", access.getAttemptsUsed());
                response.put("maxAttempts", access.getMaxAttempts());
                response.put("status", HttpStatus.FORBIDDEN.value());

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Increment attempts
            access.setAttemptsUsed(access.getAttemptsUsed() + 1);
            access.setLastWatchedAt(LocalDateTime.now());

            // If max attempts reached, hide access
            if (access.getAttemptsUsed() >= access.getMaxAttempts()) {
                access.setHasAccess(false);
            }

            videoAccessRepository.save(access);

            // Log the watch attempt
            VideoWatchLog watchLog = VideoWatchLog.builder()
                    .student(student)
                    .video(video)
                    .watchStartTime(LocalDateTime.now())
                    .completed(false)
                    .build();

            videoWatchLogRepository.save(watchLog);

            HashMap<String, Object> response = new HashMap<>();
            response.put("videoId", videoId);
            response.put("videoTitle", video.getTitle());
            response.put("attemptsUsed", access.getAttemptsUsed());
            response.put("maxAttempts", access.getMaxAttempts());
            response.put("remainingAttempts", access.getMaxAttempts() - access.getAttemptsUsed());
            response.put("hasAccessRemaining", access.getAttemptsUsed() < access.getMaxAttempts());
            response.put("message", "Attempt recorded. Remaining attempts: " +
                    (access.getMaxAttempts() - access.getAttemptsUsed()));
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to record video attempt: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getStudentVideoAccess(Long studentId, Long videoId) {
        try {
            VideoAccess access = videoAccessRepository.findByStudentIdAndVideoId(studentId, videoId)
                    .orElseThrow(() -> new RuntimeException("No access record found"));

            VideoAccessResponseDTO accessDTO = convertToAccessResponseDTO(access);

            HashMap<String, Object> response = new HashMap<>();
            response.put("access", accessDTO);
            response.put("message", "Video access details retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("studentId", studentId);
            response.put("videoId", videoId);
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve video access: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private VideoResponseDTO convertToResponseDTO(Video video) {
        try {
            return VideoResponseDTO.builder()
                    .id(video.getId())
                    .title(video.getTitle())
                    .description(video.getDescription())
                    .bunnyVideoId(video.getBunnyVideoId())
                    .videoUrl(video.getVideoUrl())
                    .thumbnailUrl(video.getThumbnailUrl())
                    .uploadMonth(video.getUploadMonth())
                    .duration(video.getDuration())
                    .fileSizeBytes(video.getFileSizeBytes())
                    .fileSizeMB(video.getFileSizeBytes() != null ?
                            String.format("%.2f MB", video.getFileSizeBytes() / (1024.0 * 1024.0)) : "N/A")
                    .originalFileName(video.getOriginalFileName())
                    .status(video.getStatus())
                    .uploadedByName(video.getUploadedBy() != null ?
                            video.getUploadedBy().getFirstName() + " " + video.getUploadedBy().getLastName() : null)
                    .isActive(video.isActive())
                    .createdAt(video.getCreatedAt())
                    .build();
        } catch (Exception e) {
            // Return minimal DTO on error
            return VideoResponseDTO.builder()
                    .id(video.getId())
                    .title(video.getTitle())
                    .bunnyVideoId(video.getBunnyVideoId())
                    .status(video.getStatus())
                    .isActive(video.isActive())
                    .build();
        }
    }

    private VideoAccessResponseDTO convertToAccessResponseDTO(VideoAccess access) {
        try {
            return VideoAccessResponseDTO.builder()
                    .id(access.getId())
                    .studentId(access.getStudent().getId())
                    .studentName(access.getStudent().getFirstName() + " " + access.getStudent().getLastName())
                    .videoId(access.getVideo().getId())
                    .videoTitle(access.getVideo().getTitle())
                    .videoUrl(access.getVideo().getVideoUrl())
                    .hasAccess(access.isHasAccess())
                    .maxAttempts(access.getMaxAttempts())
                    .attemptsUsed(access.getAttemptsUsed())
                    .remainingAttempts(access.getMaxAttempts() - access.getAttemptsUsed())
                    .lastWatchedAt(access.getLastWatchedAt())
                    .accessGrantedDate(access.getAccessGrantedDate())
                    .grantedByName(access.getGrantedBy() != null ?
                            access.getGrantedBy().getFirstName() + " " + access.getGrantedBy().getLastName() : null)
                    .notes(access.getNotes())
                    .build();
        } catch (Exception e) {
            // Return minimal DTO on error
            return VideoAccessResponseDTO.builder()
                    .id(access.getId())
                    .hasAccess(access.isHasAccess())
                    .maxAttempts(access.getMaxAttempts())
                    .attemptsUsed(access.getAttemptsUsed())
                    .remainingAttempts(access.getMaxAttempts() - access.getAttemptsUsed())
                    .build();
        }
    }

    @Override
    public ResponseEntity<?> setTotalWatchTimeToZero(Long videoId, Long userId){
        try {
            VideoAccess access = videoAccessRepository.findByStudentIdAndVideoId(userId, videoId)
                    .orElseThrow(() -> new RuntimeException("Access record not found"));

            access.setTotalSecondsWatched(0);
            videoAccessRepository.save(access);

            HashMap<String, Object> response = new HashMap<>();
            response.put("videoId", videoId);
            response.put("userId", userId);
            response.put("message", "Total watch time reset to zero successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to reset total watch time: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
