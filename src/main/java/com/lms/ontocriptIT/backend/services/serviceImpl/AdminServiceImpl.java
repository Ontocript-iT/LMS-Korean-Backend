package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.auth.RegistrationRequest;
import com.lms.ontocriptIT.backend.dtos.ApproveRegistrationDTO;
import com.lms.ontocriptIT.backend.dtos.StudentDTO;
import com.lms.ontocriptIT.backend.entity.*;
import com.lms.ontocriptIT.backend.repository.*;
import com.lms.ontocriptIT.backend.services.SmsService;
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

import java.time.LocalDate;
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

    private final PreviousBatchUserRepository previousBatchUserRepository;

    private final VideoRepository videoRepository;

    private final VideoAccessRepository videoAccessRepository;

    private final LoginHistoryRepository loginHistoryRepository;

    private final PaymentRepository paymentRepository;

    private final ClassAccessRepository classAccessRepository;

    private final VideoWatchLogRepository videoWatchLogRepository;

    private final ZoomClassRepository zoomClassRepository;

    private final SmsService smsService;

    // Temporary storage for the OTP (In a real app, use Redis or Database with expiry)
    private static final Map<String, String> systemResetOtpStorage = new HashMap<>();
    private static final String ADMIN_PHONE_NUMBER = "94705753003"; // The number receiving the OTP


    @Override
    public ResponseEntity<?> getPendingRegistrations(Pageable pageable) {
        try {
            Page<RegistrationRequest> pendingRequests =
                    registrationRequestRepository.findByStatus(RequestStatus.PENDING, pageable);

            HashMap<String, Object> response = new HashMap<>();

            response.put("data", pendingRequests.getContent());

            response.put("currentPage", pendingRequests.getNumber());
            response.put("totalItems", pendingRequests.getTotalElements()); // Important: Total DB count
            response.put("totalPages", pendingRequests.getTotalPages());

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
                    .groupName(dto.getGroupName())
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

            try {
                String mobile = user.getPhoneNumber1();
                if (mobile != null && !mobile.isEmpty()) {
                    // Construct message with Login Credentials
                    String smsMessage = String.format(
                            "ලියාපදිංචිය අනුමත කරන ලදී! ඔබගේ පරිශීලක නාමය (Username) සහ තාවකාලික මුරපදය (Temporary Password) ඔබගේ විද්\u200Dයුත් තැපෑලට (Email) එවා ඇත. එහි සඳහන් ලින්ක් (Link) එකෙන් ගොස්, ඔබට ඔබේ තාවකාලික මුරපදය (temporary password) අවශ්\u200Dය නම් වෙනස් කරගත හැක.පන්තිවලට සම්බන්ධ වීම සඳහා මුදල් ගෙවූ රිසිට් පත පහත අංකයට WhatsApp කිරීම අනිවාර්ය වේ.අංකය: 0705753003 WhatsApp Link: https://wa.me/94705753003 — Kandy EPS TOPIK"
                    );

                    // Send in background thread
                    new Thread(() -> smsService.sendSms(mobile, smsMessage)).start();
                }
            } catch (Exception e) {
                System.err.println("Failed to send approval SMS: " + e.getMessage());
            }

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

 import java.time.LocalDate;

    private String generateStudentId() {
        try {
            LocalDate currentDate = LocalDate.now();

            String prefix = "S";

            String monthPart = currentDate.getMonth().name().substring(0, 2).toUpperCase();

            String yearPart = String.format("%02d", currentDate.getYear() % 100);

            long count = userRepository.count() + 1;
            String sequencePart = String.format("%05d", count);

            return prefix + monthPart + yearPart + sequencePart;

        } catch (Exception e) {
            return "S" + System.currentTimeMillis();
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
        dto.setGroupName(user.getGroupName());
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

    @Override
    public ResponseEntity<?> searchApprovedStudentById(String studentId){

        try {
            List<User> users = userRepository
                    .findByStudentIdContainingAndRoleAndStatus(
                            studentId, Role.STUDENT, AccountStatus.ACTIVE);

            List<StudentDTO> studentDTOs = users.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", studentDTOs);
            response.put("count", studentDTOs.size());
            response.put("message", "Search completed successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to search students: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> suspendOrActiveStudent(String studentId){

        try {
            User user = userRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

            List<ClassAccess> classAccess = classAccessRepository.findByStudentId(user.getId());

            VideoAccess videoAccess = videoAccessRepository.findByStudentId(user.getId());

            if (user.getStatus() == AccountStatus.ACTIVE) {
                user.setStatus(AccountStatus.SUSPENDED);
                classAccess.forEach(access -> {
                    access.setHasAccess(false);
                    classAccessRepository.save(access);
                });
                if (videoAccess != null) {
                    videoAccess.setHasAccess(false);
                    videoAccessRepository.save(videoAccess);
                }

            } else {
                user.setStatus(AccountStatus.ACTIVE);
                classAccess.forEach(access -> {
                    access.setHasAccess(true);
                    classAccessRepository.save(access);
                });

                if (videoAccess != null) {
                    videoAccess.setHasAccess(true);
                    videoAccessRepository.save(videoAccess);
                }
            }

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("studentId", user.getStudentId());
            response.put("newStatus", user.getStatus().toString());
            response.put("message", "Student status updated successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to update student status: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public  ResponseEntity<?> getSuspendedStudents(Pageable pageable) {
        try {
            Page<User> suspendedStudentsPage =
                    userRepository.findByRoleAndStatus(Role.STUDENT, AccountStatus.SUSPENDED, pageable);

            List<StudentDTO> studentDTOs = suspendedStudentsPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("data", studentDTOs);
            response.put("count", suspendedStudentsPage.getTotalElements());
            response.put("currentPage", suspendedStudentsPage.getNumber());
            response.put("totalPages", suspendedStudentsPage.getTotalPages());
            response.put("message", "Suspended students retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve suspended students: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> clearAndResetFullSystem(String otp) {
        try {
            String storedOtp = systemResetOtpStorage.get("ADMIN_RESET_KEY");

            if (storedOtp == null || !storedOtp.equals(otp)) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "OTP අංකය වැරදියි හෝ කල් ඉකුත් වී ඇත. (Invalid or Expired OTP)");
                response.put("status", HttpStatus.BAD_REQUEST.value());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            List<User> usersToArchive = userRepository.findAllByRoleNot(Role.ADMIN);

            List<PreviousBatchUser> archiveList = usersToArchive.stream().map(user ->
                    PreviousBatchUser.builder()
                            .originalUserId(user.getId())
                            .studentId(user.getStudentId())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .district(user.getDistrict())
                            .idNumber(user.getIdNumber())
                            .email(user.getEmail())
                            .phoneNumber1(user.getPhoneNumber1())
                            .phoneNumber2(user.getPhoneNumber2())
                            .password(user.getPassword())
                            .role(user.getRole())
                            .originalCreatedAt(user.getCreatedAt())
                            .build()
            ).collect(Collectors.toList());

            if (!archiveList.isEmpty()) {
                previousBatchUserRepository.saveAll(archiveList);
            }

            zoomClassRepository.deleteAll();
            videoWatchLogRepository.deleteAll();
            videoRepository.deleteAll();
            videoAccessRepository.deleteAll();
            loginHistoryRepository.deleteAll();
            paymentRepository.deleteAll();
            classAccessRepository.deleteAll();

            userRepository.deleteAllByRoleNot(Role.ADMIN);

            registrationRequestRepository.deleteAll();

            systemResetOtpStorage.remove("ADMIN_RESET_KEY");

            Map<String, Object> response = new HashMap<>();
            response.put("message", "පද්ධතියේ දත්ත සියල්ල සාර්ථකව ඉවත් කරන ලදී (System cleared and reset successfully)");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to clear and reset system: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> initiateSystemReset() {
        try {
            // 1. Generate a 6-digit Random OTP
            String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

            // 2. Store it temporarily (Key can be fixed for single admin system)
            systemResetOtpStorage.put("ADMIN_RESET_KEY", otp);

            // 3. Prepare Sinhala SMS Message
            String smsMessage = "System Reset Alert: පද්ධතියේ සියලු දත්ත මකා දැමීමට (Reset) OTP අංකය: " + otp + ". මෙය ඔබ විසින් ඉල්ලා සිටියේ නැත්නම් මෙම පණිවිඩය නොසලකා හරින්න.";

            // 4. Send SMS (using a new Thread to avoid blocking)
            new Thread(() -> {
                try {
                    smsService.sendSms(ADMIN_PHONE_NUMBER, smsMessage);
                } catch (Exception e) {
                    System.err.println("Failed to send Admin OTP: " + e.getMessage());
                }
            }).start();

            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "OTP අංකය " + ADMIN_PHONE_NUMBER + " වෙත යවන ලදී. කරුණාකර තහවුරු කරන්න."); // OTP sent successfully
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "OTP යැවීම අසාර්ථකයි: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteRegistrationRequestBYPhoneNumber1OrEmail(String identifier) {
        try {
            RegistrationRequest requestsToDelete = registrationRequestRepository
                    .findByPhoneNumber1OrEmail(identifier, identifier);

            User existingUser = userRepository.findByPhoneNumber1OrEmail(identifier, identifier)
                    .orElse(null);

            boolean requestsExist = (requestsToDelete != null);
            boolean userExists = (existingUser != null);

            if (!requestsExist && !userExists) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No registration requests or user found with the provided identifier.");
                response.put("status", HttpStatus.NOT_FOUND.value());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (userExists) {
                Long userId = existingUser.getId();

                if (userId != null) {
                    videoWatchLogRepository.deleteByStudentId(userId);
                    videoAccessRepository.deleteByStudentId(userId);
                    paymentRepository.deleteByStudentId(userId);

                    loginHistoryRepository.deleteByUserId(userId);

                    classAccessRepository.deleteByStudentId(userId);
                }

                userRepository.delete(existingUser);
            }

            if (requestsExist) {
                registrationRequestRepository.delete(requestsToDelete);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cleanup successful.");
            response.put("userDeleted", userExists);
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to delete data: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
