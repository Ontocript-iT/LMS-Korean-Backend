package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.auth.RegistrationRequest;
import com.lms.ontocriptIT.backend.dtos.ApproveRegistrationDTO;
import com.lms.ontocriptIT.backend.dtos.StudentDTO;
import com.lms.ontocriptIT.backend.entity.AccountStatus;
import com.lms.ontocriptIT.backend.entity.RequestStatus;
import com.lms.ontocriptIT.backend.entity.Role;
import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.repository.RegistrationRequestRepository;
import com.lms.ontocriptIT.backend.repository.UserRepository;
import com.lms.ontocriptIT.backend.services.centralServices.AdminService;
import com.lms.ontocriptIT.backend.services.centralServices.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RegistrationRequestRepository registrationRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public ResponseEntity<?> getPendingRegistrations() {
        try {
            List<RegistrationRequest> pendingRequests =
                    registrationRequestRepository.findByStatus(RequestStatus.PENDING);

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", pendingRequests);
            response.put("count", pendingRequests.size());
            response.put("message", "Pending registrations retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve pending registrations: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> approveRegistration(ApproveRegistrationDTO dto) {
        try {
            RegistrationRequest request = registrationRequestRepository
                    .findById(dto.getRegistrationRequestId())
                    .orElseThrow(() -> new RuntimeException("Registration request not found"));

            if (request.getStatus() != RequestStatus.PENDING) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Request has already been processed");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Generate student ID
            String studentId = generateStudentId();

            // Generate temporary password
            String tempPassword = generateTemporaryPassword(
                    request.getFirstName(),
                    request.getIdNumber()
            );

            // Generate reset token
            String resetToken = UUID.randomUUID().toString();

            // Create user account
            User user = User.builder()
                    .studentId(studentId)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .district(request.getDistrict())
                    .idNumber(request.getIdNumber())
                    .email(request.getEmail())
                    .phoneNumber1(request.getPhoneNumber1())
                    .phoneNumber2(request.getPhoneNumber2())
                    .password(passwordEncoder.encode(tempPassword))
                    .role(Role.STUDENT)
                    .status(AccountStatus.ACTIVE)
                    .isTemporaryPassword(true)
                    .resetPasswordToken(resetToken)
                    .tokenExpiryDate(LocalDateTime.now().plusDays(7))
                    .build();

            userRepository.save(user);

            // Update registration request
            request.setStatus(RequestStatus.APPROVED);
            request.setReviewedAt(LocalDateTime.now());
            registrationRequestRepository.save(request);

            // Send email
            try {
                emailService.sendAccountCreationEmail(
                        user.getEmail(),
                        studentId,
                        tempPassword,
                        resetToken
                );
            } catch (Exception emailException) {
                System.err.println("Failed to send email: " + emailException.getMessage());
                // Continue even if email fails
            }

            HashMap<String, Object> response = new HashMap<>();
            response.put("studentId", studentId);
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("message", "Registration request approved and account created successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to approve registration: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> rejectRegistration(Long requestId) {
        try {
            RegistrationRequest request = registrationRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Registration request not found"));

            if (request.getStatus() != RequestStatus.PENDING) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Request has already been processed");
                response.put("currentStatus", request.getStatus().toString());
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            request.setStatus(RequestStatus.REJECTED);
            request.setReviewedAt(LocalDateTime.now());
            registrationRequestRepository.save(request);

            HashMap<String, Object> response = new HashMap<>();
            response.put("requestId", requestId);
            response.put("applicantName", request.getFirstName() + " " + request.getLastName());
            response.put("applicantEmail", request.getEmail());
            response.put("message", "Registration request rejected successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to reject registration: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String generateStudentId() {
        try {
            String year = String.valueOf(Year.now().getValue());
            long count = userRepository.count() + 1;
            return "STU" + year + String.format("%05d", count);

        } catch (Exception e) {
            // Fallback to timestamp-based ID if count fails
            return "STU" + Year.now().getValue() + System.currentTimeMillis() % 100000;
        }
    }

    private String generateTemporaryPassword(String firstName, String idNumber) {
        try {
            String last5Digits = idNumber.replaceAll("[^0-9]", "");
            last5Digits = last5Digits.substring(Math.max(0, last5Digits.length() - 5));
            return firstName.toLowerCase() + last5Digits;

        } catch (Exception e) {
            // Fallback password generation
            return firstName.toLowerCase() + "12345";
        }
    }

    @Override
    public ResponseEntity<?> getApprovedStudents(Pageable pageable) {
        try {
            // 1. Update repository call to use pageable
            Page<User> approvedStudentsPage =
                    userRepository.findByRoleAndStatus(Role.STUDENT, AccountStatus.ACTIVE, pageable);

            // 2. Map the Page content to DTOs
            List<StudentDTO> studentDTOs = approvedStudentsPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // 3. Build response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("data", studentDTOs);
            response.put("count", approvedStudentsPage.getTotalElements()); // Total records in DB
            response.put("currentPage", approvedStudentsPage.getNumber());
            response.put("totalPages", approvedStudentsPage.getTotalPages());
            response.put("message", "Approved students retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve approved students: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private StudentDTO convertToDTO(User user) {
        StudentDTO dto = new StudentDTO();
        dto.setId(user.getId());
        dto.setStudentId(user.getStudentId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setDistrict(user.getDistrict());
        dto.setPhoneNumber1(user.getPhoneNumber1());
        dto.setPhoneNumber2(user.getPhoneNumber2());
        dto.setStatus(user.getStatus().toString());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setPaymentCount(user.getPayments() != null ? user.getPayments().size() : 0);
        dto.setClassAccessCount(user.getClassAccesses() != null ? user.getClassAccesses().size() : 0);
        return dto;
    }

    @Override
    public ResponseEntity<?> getStudentById(String studentId){
        try {
            User user = userRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

            StudentDTO dto = convertToDTO(user);

            Map<String, Object> response = new HashMap<>();
            response.put("data", dto);
            response.put("message", "Student retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve student: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
