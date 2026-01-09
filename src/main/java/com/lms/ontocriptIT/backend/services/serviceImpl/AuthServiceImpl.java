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
import com.lms.ontocriptIT.backend.services.centralServices.AuthService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final RegistrationRequestRepository registrationRequestRepository;
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WhatsAppService whatsAppService;
    private final DeviceInfoExtractor deviceInfoExtractor;

    @Override
    @Transactional
    public ResponseEntity<?> registerStudent(StudentRegistrationDTO dto) {
        try {
            // Check for duplicates
            if (registrationRequestRepository.existsByEmail(dto.getEmail())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Email already registered");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (registrationRequestRepository.existsByIdNumber(dto.getIdNumber())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "ID number already registered");
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (userRepository.existsByEmail(dto.getEmail())) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Email already exists in system");
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

            // Send WhatsApp notification to teacher
            try {
                String studentFullName = savedRequest.getFirstName() + " " + savedRequest.getLastName();
                whatsAppService.sendNewStudentRegistrationNotification(
                        studentFullName,
                        savedRequest.getEmail(),
                        savedRequest.getPhoneNumber1(),
                        savedRequest.getIdNumber()
                );
            } catch (Exception e) {
                System.err.println("WhatsApp notification failed: " + e.getMessage());
                // Continue even if WhatsApp fails
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
    public ResponseEntity<?> login (LoginRequestDTO dto, HttpServletRequest request) {
        User user = null;
        boolean loginSuccessful = false;
        String failureReason = null;

        try {
            // Find user by identifier
            user = findUserByIdentifier(dto.getUsername())
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));

            // Extract device information FIRST
            DeviceInfoDTO deviceInfo = deviceInfoExtractor.extractDeviceInfo(request);

            // FIRST: Check if user already has active session on different device
            LoginHistory activeSession = loginHistoryRepository.findCurrentSessionByUserId(user.getId()).orElse(null);

            if (activeSession != null && !activeSession.getDeviceFingerprint().equals(deviceInfo.getDeviceFingerprint())) {
                // Invalidate previous session
                loginHistoryRepository.invalidateCurrentSessionsByUserId(user.getId());

                HashMap<String, Object> response = new HashMap<>();
                response.put("deviceAuthorized", false);
                response.put("activeSessionInvalidated", true);
                response.put("previousDevice", activeSession.getDeviceType() + " - " + activeSession.getBrowser());
                response.put("message", "Previous session invalidated. You can now login from this device.");
                response.put("status", HttpStatus.OK.value());

                return ResponseEntity.ok(response);
            }

            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );

            if (authentication.isAuthenticated()) {
                loginSuccessful = true;

                // Mark this as current session
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

                loginHistoryRepository.save(newSession);

                String token = jwtService.generateToken(user);

                HashMap<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("studentId", user.getStudentId());
                response.put("email", user.getEmail());
                response.put("firstName", user.getFirstName());
                response.put("lastName", user.getLastName());
                response.put("role", user.getRole().name());
                response.put("isTemporaryPassword", user.isTemporaryPassword());
                response.put("deviceFingerprint", deviceInfo.getDeviceFingerprint());
                response.put("deviceInfo", deviceInfo);
                response.put("sessionId", newSession.getId());
                response.put("message", user.isTemporaryPassword() ?
                        "Please change your temporary password" : "Login successful");
                response.put("status", HttpStatus.OK.value());

                return ResponseEntity.ok(response);
            }

            failureReason = "Authentication failed";

            // Record failed login
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
                    .failureReason(failureReason)
                    .build();

            loginHistoryRepository.save(failedLogin);

            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Invalid credentials");
            response.put("status", HttpStatus.UNAUTHORIZED.value());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            failureReason = e.getMessage();

            // Record failed login attempt if user exists
            if (user != null) {
                try {
                    DeviceInfoDTO deviceInfo = deviceInfoExtractor.extractDeviceInfo(request);
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
                            .failureReason(failureReason)
                            .build();

                    loginHistoryRepository.save(failedLogin);
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

            String resetToken = UUID.randomUUID().toString();
            user.setResetPasswordToken(resetToken);
            user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));

            userRepository.save(user);

            HashMap<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("message", "Password reset link sent to your email");
            response.put("expiresIn", "24 hours");
            response.put("status", HttpStatus.OK.value());

            // Send email would be handled by EmailService
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to initiate password reset: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
