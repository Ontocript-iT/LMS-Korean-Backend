package com.lms.ontocriptIT.backend.auth;

import com.lms.ontocriptIT.backend.repository.LoginHistoryRepository;
import com.lms.ontocriptIT.backend.services.centralServices.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    private final LoginHistoryRepository loginHistoryRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        return authService.login(loginRequest, request);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        return authService.resetPassword(resetPasswordDTO);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String identifier) {
        return authService.initiatePasswordReset(identifier);
    }

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
