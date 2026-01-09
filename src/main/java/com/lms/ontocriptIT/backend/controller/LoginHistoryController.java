package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.services.centralServices.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login-history")
@RequiredArgsConstructor
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> getStudentLoginHistory(@PathVariable Long studentId) {
        return loginHistoryService.getStudentLoginHistory(studentId);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRecentLogins(@RequestParam(defaultValue = "7") int days) {
        return loginHistoryService.getRecentLogins(days);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllLoginHistory() {
        return loginHistoryService.getAllLoginHistory();
    }
}
