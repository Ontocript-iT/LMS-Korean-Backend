package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.auth.*;
import com.lms.ontocriptIT.backend.dtos.DeviceInfoDTO;
import com.lms.ontocriptIT.backend.dtos.StudentRegistrationDTO;
import com.lms.ontocriptIT.backend.entity.LoginHistory;
import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.repository.LoginHistoryRepository;
import com.lms.ontocriptIT.backend.repository.RegistrationRequestRepository;
import com.lms.ontocriptIT.backend.repository.UserRepository;
import com.lms.ontocriptIT.backend.security.JwtService;
import com.lms.ontocriptIT.backend.services.SmsService;
import com.lms.ontocriptIT.backend.services.centralServices.AuthService;
import com.lms.ontocriptIT.backend.services.centralServices.EmailService;
import com.lms.ontocriptIT.backend.services.centralServices.WhatsAppService;
import com.lms.ontocriptIT.backend.utils.DeviceInfoExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final RegistrationRequestRepository registrationRequestRepository;
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    private final SmsService smsService;

    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WhatsAppService whatsAppService;
    private final DeviceInfoExtractor deviceInfoExtractor;

    @Override
    @Transactional
    public ResponseEntity<?> registerStudent(StudentRegistrationDTO dto) {
        try {
          //   Check for duplicates
            if (registrationRequestRepository.existsByEmail(dto.getEmail())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "මෙම Email ලිපිනය දැනටමත් ලියාපදිංචි කර ඇත.");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (registrationRequestRepository.existsByIdNumber(dto.getIdNumber())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "මෙම ID අංකය දැනටමත් ලියාපදිංචි කර ඇත.");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (userRepository.existsByPhoneNumber1(dto.getPhoneNumber1())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "ඔබ ඇතුළත් කළ දුරකථන අංක 01 දැනටමත් පද්ධතියේ පවතී. කරුණාකර වෙනත් දුරකථන අංකයක් භාවිතා කරන්න.");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (userRepository.existsByEmail(dto.getEmail())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "මෙම Email ලිපිනය දැනටමත් ලියාපදිංචි කර ඇත.");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Create registration request
            RegistrationRequest request = RegistrationRequest.builder()
                    .firstName(dto.getFirstName())
                    .lastName(dto.getLastName())
                    .district(dto.getDistrict())
                    .idNumber(dto.getIdNumber())
                    .email(dto.getEmail())
                    .phoneNumber1(dto.getPhoneNumber1())
                    .phoneNumber2(dto.getPhoneNumber2())
                    .build();

            RegistrationRequest savedRequest = registrationRequestRepository.save(request);

            try {
                String teacherMobile = "94705753003";
                String studentMobile = savedRequest.getPhoneNumber1();

                String studentName = savedRequest.getFirstName() + " " + savedRequest.getLastName();
                String district = savedRequest.getDistrict();

                String teacherMsg = String.format(
                        "New Registration Alert:නම: %s දිස්ත්\u200Dරික්කය: %s දුරකථන: %s අනුමත කිරීම සඳහා කරුණාකර ADMIN (Portal) වෙත පිවිසෙන්න.",
                        studentName, district, studentMobile
                );

                // Message for Student (Your specific text)
                String studentMsg = "ඔබ KANDY EPS TOPIK සමඟ එක් වීම පිළිබඳව අප සතුටු වෙමු. කරුණාකර ඔබගේ ලියාපදිංචිය තහවුරු කරන තෙක් රැඳී සිටින්න. පැය 24ක් ඇතුළත එය තහවුරු කර ඒ පිළිබඳව SMS පණිවිඩයක් ඔබට ලැබෙනු ඇත. — KANDY EPS TOPIK";

                new Thread(() -> {
                    // Send to Teacher
                    if (teacherMobile != null && !teacherMobile.isEmpty()) {
                        smsService.sendSms(teacherMobile, teacherMsg);
                    }

                    // Send to Student
                    if (studentMobile != null && !studentMobile.isEmpty()) {
                        smsService.sendSms(studentMobile, studentMsg);
                    }
                }).start();

            } catch (Exception e) {
                System.err.println("SMS notification failed: " + e.getMessage());
            }


            HashMap<String, Object> response = new HashMap<>();
            response.put("requestId", savedRequest.getId());
            response.put("applicantName", savedRequest.getFirstName() + " " + savedRequest.getLastName());
            response.put("email", savedRequest.getEmail());
            response.put("message", "Registration request submitted successfully. Please wait for admin approval.");
            response.put("status", HttpStatus.CREATED.value());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to submit registration: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> login(LoginRequestDTO dto, HttpServletRequest request) {
        User user = null;
        boolean loginSuccessful = false;
        String failureReason = null;

        try {
            user = findUserByIdentifier(dto.getUsername())
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));

            DeviceInfoDTO deviceInfo = deviceInfoExtractor.extractDeviceInfo(request);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );

            if (authentication.isAuthenticated()) {
                loginSuccessful = true;
                LoginHistory currentSessionRecord; // This will hold the record we save/update

                LoginHistory activeSession = loginHistoryRepository.findCurrentSessionByUserId(user.getId()).orElse(null);

                // Active Session Exists on SAME Device ---
                if (activeSession != null && activeSession.getDeviceFingerprint().equals(deviceInfo.getDeviceFingerprint())) {

                    // UPDATE the existing record instead of creating a new one
                    activeSession.setLoginTime(LocalDateTime.now());
                    activeSession.setIpAddress(deviceInfo.getIpAddress()); // Update IP in case they moved network
                    activeSession.setLoginSuccessful(true);

                    // Save the update
                    currentSessionRecord = loginHistoryRepository.save(activeSession);

                }
                // New Device OR No Active Session ---
                else {
                    // If there was an active session on a DIFFERENT device, invalidate it first
                    if (activeSession != null) {
                        loginHistoryRepository.invalidateCurrentSessionsByUserId(user.getId());
                    }

                    // --- SMS ALERT LOGIC (Only runs for new sessions/devices) ---
                    try {
                        Optional<LoginHistory> lastLoginOpt = loginHistoryRepository
                                .findTopByUserIdAndLoginSuccessfulOrderByLoginTimeDesc(user.getId(), true);

                        if (lastLoginOpt.isPresent()) {
                            LoginHistory lastLogin = lastLoginOpt.get();

                            // Check if device is different
                            if (!lastLogin.getDeviceFingerprint().equals(deviceInfo.getDeviceFingerprint())) {
                                String studentMobile = user.getPhoneNumber1();
                                String studentName = user.getFirstName();
                                String studentId = user.getStudentId(); // Assuming you have this field
                                String deviceType = deviceInfo.getDeviceType();

                                String teacherMobile = "94705753003";

                                String studentMsg = String.format(
                                        "Student Alert: ඔබ ( %s) පද්ධතියට පිවිසීම සඳහා වෙනත් උපාංගයක් (%s) භාවිතා කරන බව අපට නිරීක්ෂණය වේ. මෙය දිගින් දිගටම සිදු කළහොත්, ඔබව පද්ධතියෙන් ඉවත් කිරීමට සිදුවන බව කරුණාවෙන් සලකන්න.--Danister Serasighne,Kandy Eps Topik",
                                        studentName, deviceType);

                                String teacherMsg = String.format(
                                        "Admin Alert: %s (%s) සිසුවා නව උපාංගයක් (%s) මගින් පද්ධතියට පිවිස ඇත. අවසරයකින් තොරව ගිණුම හුවමාරු කරගැනීමක් සිදුවන්නේදැයි කරුණාකර විමර්ශනය කරන්න",
                                        studentName, studentId, deviceType);

                                //Send SMS Asynchronously (Both messages in one thread)
                                new Thread(() -> {
                                    // Send to Student
                                    if (studentMobile != null && !studentMobile.isEmpty()) {
                                        smsService.sendSms(studentMobile, studentMsg);
                                    }

                                    // Send to Teacher (Fixed Number)
                                    if (teacherMobile != null && !teacherMobile.isEmpty()) {
                                        smsService.sendSms(teacherMobile, teacherMsg);
                                    }
                                }).start();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("SMS Alert Failed: " + e.getMessage());
                    }
                    LoginHistory newSession = LoginHistory.builder()
                            .user(user)
                            .loginTime(LocalDateTime.now())
                            .ipAddress(deviceInfo.getIpAddress())
                            .deviceType(deviceInfo.getDeviceType())
                            .browser(deviceInfo.getBrowser())
                            .operatingSystem(deviceInfo.getOperatingSystem())
                            .userAgent(deviceInfo.getUserAgent())
                            .deviceFingerprint(deviceInfo.getDeviceFingerprint())
                            .isCurrentSession(true)
                            .loginSuccessful(true)
                            .build();

                    currentSessionRecord = loginHistoryRepository.save(newSession);
                }

                //Generate Token and Response
                String token = jwtService.generateToken(user);

                HashMap<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("id", user.getId());
                response.put("studentId", user.getStudentId());
                response.put("email", user.getEmail());
                response.put("firstName", user.getFirstName());
                response.put("lastName", user.getLastName());
                response.put("role", user.getRole().name());
                response.put("deviceFingerprint", deviceInfo.getDeviceFingerprint());
                response.put("sessionId", currentSessionRecord.getId()); // ID of updated or new session
                response.put("message", "Login successful");
                response.put("status", HttpStatus.OK.value());

                return ResponseEntity.ok(response);
            }

            // --- Handle Authentication Failure ---
            failureReason = "Authentication failed";
            saveFailedLogin(user, deviceInfo, failureReason);

            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Invalid credentials");
            response.put("status", HttpStatus.UNAUTHORIZED.value());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            failureReason = e.getMessage();
            if (user != null) {
                try {
                    DeviceInfoDTO deviceInfo = deviceInfoExtractor.extractDeviceInfo(request);
                    saveFailedLogin(user, deviceInfo, failureReason);
                } catch (Exception ex) {
                    System.err.println("Failed to record login history: " + ex.getMessage());
                }
            }
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Invalid credentials");
            response.put("status", HttpStatus.UNAUTHORIZED.value());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // Helper method to keep code clean
    private void saveFailedLogin(User user, DeviceInfoDTO deviceInfo, String reason) {
        String safeReason = reason != null && reason.length() > 990 ? reason.substring(0, 990) : reason;

        LoginHistory failedLogin = LoginHistory.builder()
                .user(user)
                .loginTime(LocalDateTime.now())
                .ipAddress(deviceInfo.getIpAddress())
                .deviceType(deviceInfo.getDeviceType())
                .browser(deviceInfo.getBrowser())
                .operatingSystem(deviceInfo.getOperatingSystem())
                .userAgent(deviceInfo.getUserAgent())
                .deviceFingerprint(deviceInfo.getDeviceFingerprint())
                .isCurrentSession(false)
                .loginSuccessful(false)
                .failureReason(safeReason)
                .build();
        loginHistoryRepository.save(failedLogin);
    }


//    private void recordLoginHistory(User user, DeviceInfoDTO deviceInfo, boolean successful, String failureReason) {
//        try {
//            LoginHistory loginHistory = LoginHistory.builder()
//                    .user(user)
//                    .loginTime(LocalDateTime.now())
//                    .ipAddress(deviceInfo.getIpAddress())
//                    .deviceType(deviceInfo.getDeviceType())
//                    .browser(deviceInfo.getBrowser())
//                    .operatingSystem(deviceInfo.getOperatingSystem())
//                    .userAgent(deviceInfo.getUserAgent())
//                    .loginSuccessful(successful)
//                    .failureReason(failureReason)
//                    .build();
//
//            loginHistoryRepository.save(loginHistory);
//        } catch (Exception e) {
//            System.err.println("Failed to save login history: " + e.getMessage());
//        }
//    }

    @Override
    @Transactional
    public ResponseEntity<?> resetPassword(ResetPasswordDTO dto) {
        try {
            // Validate passwords match
            if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Passwords do not match");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Find user by token
            User user = userRepository.findByResetPasswordToken(dto.getToken())
                    .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

            // Check token expiry
            if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Reset token has expired");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Verify current password
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Current password is incorrect");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Update password
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
            user.setTemporaryPassword(false);
            user.setResetPasswordToken(null);
            user.setTokenExpiryDate(null);

            userRepository.save(user);

            HashMap<String, Object> response = new HashMap<>();
            response.put("studentId", user.getStudentId());
            response.put("email", user.getEmail());
            response.put("message", "Password reset successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.BAD_REQUEST.value());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to reset password: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> initiatePasswordReset(String identifier) {
        try {
            User user = findUserByIdentifier(identifier)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 1. Generate 6-digit OTP
            String otp = String.format("%06d", new Random().nextInt(999999));

            // 2. Store OTP in the existing reset token field
            user.setResetPasswordToken(otp);
            // OTPs should expire quickly (e.g., 15 minutes), not 24 hours
            user.setTokenExpiryDate(LocalDateTime.now().plusMinutes(15));

            userRepository.save(user);

            // 3. Send OTP via Email
            try {
                emailService.sendPasswordResetOtp(user.getEmail(), otp);
            } catch (Exception emailEx) {
                // Log error but don't fail the request to avoid revealing user existence
                System.err.println("Failed to send OTP email: " + emailEx.getMessage());
            }

            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "OTP sent to your email successfully");
            response.put("email", user.getEmail()); // optional, helpful for frontend
            response.put("expiresIn", "15 minutes");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Error: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> sendPasswordResetEmail(String studentId){
        try {
            User user = userRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate a secure token (you can use UUID or any secure random generator)
            String resetToken = UUID.randomUUID().toString();

            String tempPassword = generateTemporaryPassword(
                    user.getFirstName(),
                    user.getIdNumber()
            );

            // Store the token and its expiry in the user entity
            user.setPassword(passwordEncoder.encode(tempPassword));
            user.setResetPasswordToken(resetToken);
            user.setTokenExpiryDate(LocalDateTime.now().plusHours(24)); // Token valid for 24 hours

            userRepository.save(user);

            System.out.println("Generated reset token: " + resetToken);

            // Send password reset email
            emailService.sendPasswordResetEmail(user.getEmail(), user.getStudentId(), tempPassword, resetToken);

            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Password reset email sent successfully");
            response.put("email", user.getEmail());
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to send password reset email: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String generateTemporaryPassword(String firstName, String idNumber) {
        try {
            String last5Digits = idNumber.replaceAll("[^0-9]", "");
            last5Digits = last5Digits.substring(Math.max(0, last5Digits.length() - 4));
            return firstName.toLowerCase() + last5Digits;

        } catch (Exception e) {
            // Fallback password generation
            return firstName.toLowerCase() + "12345";
        }
    }

//    @Override
//    @Transactional
//    public ResponseEntity<?> login(LoginRequestDTO dto, HttpServletRequest request) {
//        User user = null;
//        boolean loginSuccessful = false;
//        String failureReason = null;
//
//        try {
//            // Extract device information FIRST
//            DeviceInfoDTO deviceInfo = deviceInfoExtractor.extractDeviceInfo(request);
//
//            // Find user by identifier
//            user = findUserByIdentifier(dto.getUsername())
//                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));
//
//            // Check if user already has active session on DIFFERENT device
//            Optional<LoginHistory> activeSession = loginHistoryRepository.findCurrentSessionByUserId(user.getId());
//
//            if (activeSession.isPresent() && !activeSession.get().getDeviceFingerprint().equals(deviceInfo.getDeviceFingerprint())) {
//                // Invalidate previous session BEFORE proceeding
//                loginHistoryRepository.invalidateCurrentSessionsByUserId(user.getId());
//
//                HashMap<String, Object> response = new HashMap<>();
//                response.put("deviceAuthorized", false);
//                response.put("activeSessionInvalidated", true);
//                response.put("previousDevice", activeSession.get().getDeviceType() + " - " + activeSession.get().getBrowser());
//                response.put("previousSessionId", activeSession.get().getId());
//                response.put("message", "Previous session invalidated. You can now login from this device.");
//                response.put("status", HttpStatus.OK.value());
//
//                return ResponseEntity.ok(response);
//            }
//
//            // Authenticate
//            Authentication authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
//            );
//
//            if (authentication.isAuthenticated()) {
//                loginSuccessful = true;
//
//                // Invalidate any existing sessions first
//                loginHistoryRepository.invalidateCurrentSessionsByUserId(user.getId());
//
//                // Create and save new session
//                LoginHistory newSession = LoginHistory.builder()
//                        .user(user)
//                        .loginTime(LocalDateTime.now())
//                        .ipAddress(deviceInfo.getIpAddress())
//                        .deviceType(deviceInfo.getDeviceType())
//                        .browser(deviceInfo.getBrowser())
//                        .operatingSystem(deviceInfo.getOperatingSystem())
//                        .userAgent(deviceInfo.getUserAgent())
//                        .deviceFingerprint(deviceInfo.getDeviceFingerprint())
//                        .isCurrentSession(true) // Mark as current session
//                        .loginSuccessful(true)
//                        .build();
//
//                LoginHistory savedSession = loginHistoryRepository.save(newSession);
//
//                String token = jwtService.generateToken(user);
//
//                HashMap<String, Object> response = new HashMap<>();
//                response.put("token", token);
//                response.put("studentId", user.getStudentId());
//                response.put("email", user.getEmail());
//                response.put("firstName", user.getFirstName());
//                response.put("lastName", user.getLastName());
//                response.put("role", user.getRole().name());
//                response.put("isTemporaryPassword", user.isTemporaryPassword());
//                response.put("deviceFingerprint", deviceInfo.getDeviceFingerprint());
//                response.put("sessionId", savedSession.getId());
//                response.put("deviceInfo", deviceInfo);
//                response.put("message", user.isTemporaryPassword() ?
//                        "Please change your temporary password" : "Login successful from authorized device");
//                response.put("status", HttpStatus.OK.value());
//
//                return ResponseEntity.ok(response);
//            }
//
//            failureReason = "Authentication failed";
//
//            // Record failed login
//            LoginHistory failedLogin = LoginHistory.builder()
//                    .user(user)
//                    .loginTime(LocalDateTime.now())
//                    .ipAddress(deviceInfo.getIpAddress())
//                    .deviceType(deviceInfo.getDeviceType())
//                    .browser(deviceInfo.getBrowser())
//                    .operatingSystem(deviceInfo.getOperatingSystem())
//                    .userAgent(deviceInfo.getUserAgent())
//                    .deviceFingerprint(deviceInfo.getDeviceFingerprint())
//                    .isCurrentSession(false)
//                    .loginSuccessful(false)
//                    .failureReason(failureReason)
//                    .build();
//
//            loginHistoryRepository.save(failedLogin);
//
//            HashMap<String, Object> response = new HashMap<>();
//            response.put("message", "Invalid credentials");
//            response.put("status", HttpStatus.UNAUTHORIZED.value());
//
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
//
//        } catch (Exception e) {
//            failureReason = e.getMessage();
//
//            if (user != null) {
//                try {
//                    DeviceInfoDTO deviceInfo = deviceInfoExtractor.extractDeviceInfo(request);
//                    LoginHistory failedLogin = LoginHistory.builder()
//                            .user(user)
//                            .loginTime(LocalDateTime.now())
//                            .ipAddress(deviceInfo.getIpAddress())
//                            .deviceType(deviceInfo.getDeviceType())
//                            .browser(deviceInfo.getBrowser())
//                            .operatingSystem(deviceInfo.getOperatingSystem())
//                            .userAgent(deviceInfo.getUserAgent())
//                            .deviceFingerprint(deviceInfo.getDeviceFingerprint())
//                            .isCurrentSession(false)
//                            .loginSuccessful(false)
//                            .failureReason(failureReason)
//                            .build();
//
//                    loginHistoryRepository.save(failedLogin);
//                } catch (Exception ex) {
//                    System.err.println("Failed to record login history: " + ex.getMessage());
//                }
//            }
//
//            HashMap<String, Object> response = new HashMap<>();
//            response.put("message", "Login failed: " + e.getMessage());
//            response.put("status", HttpStatus.UNAUTHORIZED.value());
//
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
//        }
//    }


    private java.util.Optional<User> findUserByIdentifier(String identifier) {
        try {
            return userRepository.findByStudentId(identifier)
                    .or(() -> userRepository.findByEmail(identifier))
                    .or(() -> userRepository.findByPhoneNumber1(identifier))
                    .or(() -> userRepository.findByPhoneNumber2(identifier));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }
}
