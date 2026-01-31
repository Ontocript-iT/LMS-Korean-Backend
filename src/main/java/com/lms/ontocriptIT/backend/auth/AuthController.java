package com.lms.ontocriptIT.backend.auth;

import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.repository.LoginHistoryRepository;
import com.lms.ontocriptIT.backend.services.centralServices.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final LoginHistoryRepository loginHistoryRepository;


    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        return authService.login(loginRequest, request);
    }

    @PostMapping("/reset-default-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        return authService.resetPassword(resetPasswordDTO);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String identifier) {
        return authService.initiatePasswordReset(identifier);
    }

    @PostMapping("/send-password-reset-email")
    public ResponseEntity<?> sendPasswordResetEmail(@RequestParam String studentId) {
        return authService.sendPasswordResetEmail(studentId);
    }

//    @PostMapping("/reset-password")
//    public ResponseEntity<?> verifyAndResetPassword(@RequestBody ResetPasswordRequest request) {
//        try {
//            // 1. Find the user
//            User user = findUserByIdentifier(request.getIdentifier())
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            // 2. Check if OTP matches
//            if (user.getResetPasswordToken() == null || !user.getResetPasswordToken().equals(request.getOtp())) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(Collections.singletonMap("message", "Invalid OTP"));
//            }
//
//            // 3. Check if OTP is expired
//            if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(Collections.singletonMap("message", "OTP has expired. Please request a new one."));
//            }
//
//            // 4. Update the password
//            // IMPORTANT: Always hash the password before saving!
//            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//
//            // 5. Clear the token so it cannot be used again
//            user.setResetPasswordToken(null);
//            user.setTokenExpiryDate(null);
//
//            userRepository.save(user);
//
//            // 6. Success Response
//            Map<String, Object> response = new HashMap<>();
//            response.put("status", HttpStatus.OK.value());
//            response.put("message", "Password has been successfully reset. You can now login.");
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            Map<String, Object> response = new HashMap<>();
//            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
//            response.put("message", "Error resetting password: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }



    @DeleteMapping("/logout")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> logout(@RequestParam(required = false) Long userId,
                                    @RequestParam(required = false) String deviceFingerprint,
                                    HttpServletRequest request) {

        try {
            Long targetUserId;
            String targetFingerprint;

            // If params provided, use them
            if (userId != null && deviceFingerprint != null) {
                targetUserId = userId;
                targetFingerprint = deviceFingerprint;
            } else {
                // Extract from JWT token or request
                // For now, invalidate all sessions for user (ADMIN only)
                targetUserId = 1L; // Extract from SecurityContextHolder
                targetFingerprint = null;
            }

            int updatedRows;
            if (targetFingerprint != null) {
                updatedRows = loginHistoryRepository.invalidateSessionByFingerprint(targetFingerprint);
            } else {
                updatedRows = loginHistoryRepository.invalidateCurrentSessionsByUserId(targetUserId);
            }

            HashMap<String, Object> response = new HashMap<>();
            response.put("sessionsInvalidated", updatedRows);
            response.put("userId", targetUserId);
            response.put("message", updatedRows > 0 ?
                    "Logged out successfully from " + updatedRows + " device(s)" :
                    "No active sessions found");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Logout failed: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


}
