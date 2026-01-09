package com.lms.ontocriptIT.backend.services.centralServices;

import org.springframework.http.ResponseEntity;

public interface LoginHistoryService {
    ResponseEntity<?> getStudentLoginHistory(Long studentId);
    ResponseEntity<?> getRecentLogins(int days);
    ResponseEntity<?> getAllLoginHistory();
}