package com.lms.ontocriptIT.backend.services.centralServices;

import com.lms.ontocriptIT.backend.dtos.VideoAccessDTO;
import com.lms.ontocriptIT.backend.dtos.VideoUploadDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VideoService {
    ResponseEntity<?> uploadVideo(String title, String description, MultipartFile file, Long adminId);
    ResponseEntity<?> getAllVideos();
    ResponseEntity<?> getActiveVideos();
    ResponseEntity<?> getVideoById(Long videoId);
    ResponseEntity<?> deleteVideo(Long videoId, Long adminId);
    ResponseEntity<?> grantVideoAccess(VideoAccessDTO dto, Long adminId);
    ResponseEntity<?> revokeVideoAccess(Long accessId, Long adminId, String notes);
    ResponseEntity<?> getStudentVideos(Long studentId);
    ResponseEntity<?> getSecureVideoEmbed(Long videoId, Long studentId);
    ResponseEntity<?> recordVideoAttempt(Long videoId, Long studentId);
    ResponseEntity<?> getStudentVideoAccess(Long studentId, Long videoId);

    Map<String, Object> trackWatchTime(Long userId, Long videoId, int heartbeatInterval);

    boolean canWatch(Long userId, Long videoId);

    ResponseEntity<?> setTotalWatchTimeToZero(Long videoId, Long id);
}
