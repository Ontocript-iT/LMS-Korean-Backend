package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.entity.LoginHistory;
import com.lms.ontocriptIT.backend.repository.LoginHistoryRepository;
import com.lms.ontocriptIT.backend.services.centralServices.DeviceVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class DeviceVerificationServiceImpl implements DeviceVerificationService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    public ResponseEntity<?> verifyDevice(Long userId, String deviceFingerprint) {
        try {
            // Check if this device has a current session for this user
            LoginHistory currentSession = loginHistoryRepository
                    .findCurrentSessionByUserIdAndFingerprint(userId, deviceFingerprint)
                    .orElse(null);

            if (currentSession != null) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("deviceAuthorized", true);
                response.put("sessionId", currentSession.getId());
                response.put("loginTime", currentSession.getLoginTime());
                response.put("message", "Device is authorized");
                response.put("status", 200);

                return ResponseEntity.ok(response);
            }

            // Check if user has an active session with different device
            LoginHistory activeSession = loginHistoryRepository.findCurrentSessionByUserId(userId).orElse(null);

            if (activeSession != null) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("deviceAuthorized", false);
                response.put("activeSession", activeSession.getId());
                response.put("activeDeviceFingerprint", activeSession.getDeviceFingerprint());
                response.put("activeDevice", activeSession.getDeviceType() + " - " + activeSession.getBrowser());
                response.put("message", "Another device is already logged in. Please logout from other device first.");
                response.put("status", 409); // Conflict

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // No active sessions - device is authorized
            HashMap<String, Object> response = new HashMap<>();
            response.put("deviceAuthorized", true);
            response.put("message", "No active sessions found. Device authorized.");
            response.put("status", 200);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Device verification failed: " + e.getMessage());
            response.put("status", 500);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
