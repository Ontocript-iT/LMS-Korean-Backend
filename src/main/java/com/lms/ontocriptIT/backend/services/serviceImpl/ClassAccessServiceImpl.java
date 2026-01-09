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

            // Check if access already exists for this class
            ClassAccess existingAccess = classAccessRepository
                    .findByStudentAndClassName(student, dto.getClassName())
                    .orElse(null);

            if (existingAccess != null) {
                // Update existing access
                existingAccess.setHasAccess(dto.isHasAccess());
                existingAccess.setClassLink(dto.getClassLink());
                existingAccess.setNotes(dto.getNotes());

                if (dto.isHasAccess()) {
                    existingAccess.setAccessGrantedDate(LocalDateTime.now());
                    existingAccess.setGrantedBy(admin);
                    existingAccess.setAccessRevokedDate(null);
                    existingAccess.setRevokedBy(null);
                }

                ClassAccess savedAccess = classAccessRepository.save(existingAccess);

                HashMap<String, Object> response = new HashMap<>();
                response.put("accessId", savedAccess.getId());
                response.put("studentId", student.getId());
                response.put("studentName", student.getFirstName() + " " + student.getLastName());
                response.put("className", savedAccess.getClassName());
                response.put("classLink", savedAccess.getClassLink());
                response.put("hasAccess", savedAccess.isHasAccess());
                response.put("grantedBy", admin.getFirstName() + " " + admin.getLastName());
                response.put("message", "Class access updated successfully");
                response.put("status", HttpStatus.OK.value());

                return ResponseEntity.ok(response);
            }

            // Create new access
            ClassAccess classAccess = ClassAccess.builder()
                    .student(student)
                    .classLink(dto.getClassLink())
                    .className(dto.getClassName())
                    .zoomClassId(dto.getZoomClassId())
                    .hasAccess(dto.isHasAccess())
                    .accessGrantedDate(dto.isHasAccess() ? LocalDateTime.now() : null)
                    .grantedBy(dto.isHasAccess() ? admin : null)
                    .notes(dto.getNotes())
                    .build();


            ClassAccess savedAccess = classAccessRepository.save(classAccess);

            List<Video> activeVideos = videoRepository.findAllActiveVideosOrderByCreatedDesc();

            for (Video video : activeVideos) {
                // Check if access already exists for this specific video and student
                VideoAccess existingVideoAccess = videoAccessRepository
                        .findByStudentAndVideo(student, video)
                        .orElse(null);

                if (existingVideoAccess != null) {
                    // Update logic
                    existingVideoAccess.setHasAccess(dto.isHasAccess());
                    if (dto.isHasAccess()) {
                        existingVideoAccess.setAccessGrantedDate(LocalDateTime.now());
                        existingVideoAccess.setGrantedBy(admin);
                        existingVideoAccess.setAccessRevokedDate(null);
                        existingVideoAccess.setRevokedBy(null);
                    }
                    videoAccessRepository.save(existingVideoAccess);
                } else {
                    // Create new logic
                    VideoAccess newVideoAccess = VideoAccess.builder()
                            .student(student)
                            .video(video)
                            .hasAccess(dto.isHasAccess())
                            .maxAttempts(2) // Default as per your requirement
                            .attemptsUsed(0)
                            .accessGrantedDate(dto.isHasAccess() ? LocalDateTime.now() : null)
                            .grantedBy(dto.isHasAccess() ? admin : null)
                            .notes("Automatically granted via class access: " + dto.getClassName())
                            .build();
                    videoAccessRepository.save(newVideoAccess);
                }
            }

            HashMap<String, Object> response = new HashMap<>();
            response.put("accessId", savedAccess.getId());
            response.put("studentId", student.getId());
            response.put("studentName", student.getFirstName() + " " + student.getLastName());
            response.put("className", savedAccess.getClassName());
            response.put("classLink", savedAccess.getClassLink());
            response.put("hasAccess", savedAccess.isHasAccess());
            response.put("grantedBy", admin.getFirstName() + " " + admin.getLastName());
            response.put("message", "Class access granted successfully");
            response.put("status", HttpStatus.CREATED.value());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to grant class access: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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
            List<ClassAccess> accesses = classAccessRepository.findByStudentId(studentId);

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
