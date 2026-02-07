package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.dtos.ClassAccessDTO;
import com.lms.ontocriptIT.backend.dtos.ClassAccessResponseDTO;
import com.lms.ontocriptIT.backend.entity.*;
import com.lms.ontocriptIT.backend.repository.*;
import com.lms.ontocriptIT.backend.services.centralServices.ClassAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassAccessServiceImpl implements ClassAccessService {

    private final ClassAccessRepository classAccessRepository;
    private final UserRepository userRepository;

    private final ZoomClassRepository zoomClassRepository;

    private final VideoServiceImpl videoServiceImpl;

    private final VideoRepository videoRepository;

    private final VideoAccessRepository videoAccessRepository;

    @Override
    @Transactional
    public ResponseEntity<?> grantClassAccess(ClassAccessDTO dto, Long adminId) {
        try {
            User student = userRepository.findById(dto.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            ClassAccess classAccess = classAccessRepository
                    .findByStudentAndClassName(student, dto.getClassName())
                    .orElse(new ClassAccess());

            boolean isNew = (classAccess.getId() == null);

            classAccess.setStudent(student);
            classAccess.setClassName(dto.getClassName());
            classAccess.setClassLink(dto.getClassLink());
            classAccess.setZoomClassId(dto.getZoomClassId());
            classAccess.setNotes(dto.getNotes());
            classAccess.setHasAccess(dto.isHasAccess());

            if (dto.isHasAccess()) {
                classAccess.setAccessGrantedDate(LocalDateTime.now());
                classAccess.setGrantedBy(admin);
                classAccess.setAccessRevokedDate(null);
            } else {
                classAccess.setAccessRevokedDate(LocalDateTime.now());
                // Optional: classAccess.setRevokedBy(admin);
            }

            ClassAccess savedAccess = classAccessRepository.save(classAccess);

//            syncVideoAccess(student, admin, dto.isHasAccess(), dto.getClassName());

            Map<String, Object> response = new HashMap<>();
            response.put("accessId", savedAccess.getId());
            response.put("studentId", student.getId());
            response.put("studentName", student.getFirstName() + " " + student.getLastName());
            response.put("className", savedAccess.getClassName());
            response.put("hasAccess", savedAccess.isHasAccess());
            response.put("message", isNew ? "Access created" : "Access updated");

            return ResponseEntity.status(isNew ? HttpStatus.CREATED : HttpStatus.OK).body(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return buildErrorResponse("Internal error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void syncVideoAccess(User student, User admin, boolean hasAccess, String className) {
        List<Video> activeVideos = videoRepository.findAllActiveVideosOrderByCreatedDesc();

        for (Video video : activeVideos) {
            VideoAccess videoAccess = videoAccessRepository
                    .findByStudentAndVideo(student, video)
                    .orElse(VideoAccess.builder()
                            .student(student)
                            .video(video)
                            .maxAttempts(2)
                            .attemptsUsed(0)
                            .hasAccess(true).build());

            videoAccess.setHasAccess(hasAccess);
            if (hasAccess) {
                videoAccess.setAccessGrantedDate(LocalDateTime.now());
                videoAccess.setGrantedBy(admin);
                videoAccess.setAccessRevokedDate(null);
                videoAccess.setNotes("Granted via class: " + className);
            } else {
                videoAccess.setAccessRevokedDate(LocalDateTime.now());
            }
            videoAccessRepository.save(videoAccess);
        }
    }

    private ResponseEntity<?> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("status", status.value());
        return ResponseEntity.status(status).body(response);
    }

    @Override
    @Transactional
    public ResponseEntity<?> revokeClassAccess(Long accessId, Long adminId, String notes) {
        try {
            ClassAccess classAccess = classAccessRepository.findById(accessId)
                    .orElseThrow(() -> new RuntimeException("Class access not found"));

            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            classAccess.setHasAccess(false);
            classAccess.setAccessRevokedDate(LocalDateTime.now());
            classAccess.setRevokedBy(admin);
            classAccess.setNotes(notes);

            ClassAccess savedAccess = classAccessRepository.save(classAccess);

            HashMap<String, Object> response = new HashMap<>();
            response.put("accessId", savedAccess.getId());
            response.put("studentId", savedAccess.getStudent().getId());
            response.put("studentName", savedAccess.getStudent().getFirstName() + " " + savedAccess.getStudent().getLastName());
            response.put("className", savedAccess.getClassName());
            response.put("hasAccess", false);
            response.put("revokedBy", admin.getFirstName() + " " + admin.getLastName());
            response.put("revokedDate", savedAccess.getAccessRevokedDate());
            response.put("notes", notes);
            response.put("message", "Class access revoked successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to revoke class access: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getStudentClassAccesses(Long studentId) {
        try {
            List<ClassAccess> accesses = classAccessRepository.findByStudentIdAndHasAccessTrue(studentId);

            List<ClassAccessResponseDTO> responseDTOs = accesses.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", responseDTOs);
            response.put("count", responseDTOs.size());
            response.put("studentId", studentId);
            response.put("message", "Class accesses retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve class accesses: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getAllClassAccesses() {
        try {
            List<ClassAccess> accesses = classAccessRepository.findAll();

            List<ClassAccessResponseDTO> responseDTOs = accesses.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", responseDTOs);
            response.put("count", responseDTOs.size());
            response.put("message", "All class accesses retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve all class accesses: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateClassAccess(Long accessId, ClassAccessDTO dto, Long adminId) {
        try {
            ClassAccess classAccess = classAccessRepository.findById(accessId)
                    .orElseThrow(() -> new RuntimeException("Class access not found"));

            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            classAccess.setClassLink(dto.getClassLink());
            classAccess.setClassName(dto.getClassName());
            classAccess.setNotes(dto.getNotes());

            boolean previousAccess = classAccess.isHasAccess();
            classAccess.setHasAccess(dto.isHasAccess());

            if (!previousAccess && dto.isHasAccess()) {
                classAccess.setAccessGrantedDate(LocalDateTime.now());
                classAccess.setGrantedBy(admin);
            } else if (previousAccess && !dto.isHasAccess()) {
                classAccess.setAccessRevokedDate(LocalDateTime.now());
                classAccess.setRevokedBy(admin);
            }

            ClassAccess savedAccess = classAccessRepository.save(classAccess);

            HashMap<String, Object> response = new HashMap<>();
            response.put("accessId", savedAccess.getId());
            response.put("studentId", savedAccess.getStudent().getId());
            response.put("studentName", savedAccess.getStudent().getFirstName() + " " + savedAccess.getStudent().getLastName());
            response.put("className", savedAccess.getClassName());
            response.put("classLink", savedAccess.getClassLink());
            response.put("hasAccess", savedAccess.isHasAccess());
            response.put("updatedBy", admin.getFirstName() + " " + admin.getLastName());
            response.put("message", "Class access updated successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to update class access: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private ClassAccessResponseDTO convertToResponseDTO(ClassAccess classAccess) {
        try {
            Optional<ZoomClass> zoomClass= zoomClassRepository.findById(classAccess.getZoomClassId());

            System.out.println("ssss-"+zoomClass.get().getClassTime());

            return ClassAccessResponseDTO.builder()
                    .id(classAccess.getId())
                    .studentId(classAccess.getStudent().getId())
                    .studentName(classAccess.getStudent().getFirstName() + " " + classAccess.getStudent().getLastName())
                    .classLink(classAccess.getClassLink())
                    .className(classAccess.getClassName())
                    .hasAccess(classAccess.isHasAccess())
                    .classDate(zoomClass.get().getClassDate())
                    .classTime(zoomClass.get().getClassTime())
                    .accessGrantedDate(classAccess.getAccessGrantedDate())
                    .accessRevokedDate(classAccess.getAccessRevokedDate())
                    .grantedByName(classAccess.getGrantedBy() != null ?
                            classAccess.getGrantedBy().getFirstName() + " " + classAccess.getGrantedBy().getLastName() : null)
                    .revokedByName(classAccess.getRevokedBy() != null ?
                            classAccess.getRevokedBy().getFirstName() + " " + classAccess.getRevokedBy().getLastName() : null)
                    .notes(classAccess.getNotes())
                    .build();
        } catch (Exception e) {
            // Return minimal DTO on error
            return ClassAccessResponseDTO.builder()
                    .id(classAccess.getId())
                    .className(classAccess.getClassName())
                    .hasAccess(classAccess.isHasAccess())
                    .build();
        }
    }
}
