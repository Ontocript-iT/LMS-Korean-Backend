package com.lms.ontocriptIT.backend.services.centralServices;

import org.springframework.http.ResponseEntity;

public interface DeviceVerificationService {
    ResponseEntity<?> verifyDevice(Long userId, String deviceFingerprint);
}
