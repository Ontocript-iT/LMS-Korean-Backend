package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.entity.User;
//import com.lms.ontocriptIT.backend.services.centralServices.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

//    private final StudentService studentService;


    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok("Welcome to student dashboard, " + user.getFirstName());
    }

    @GetMapping("/getApprovedStudents")
    public ResponseEntity<?> getApprovedStudents() {
        // Placeholder implementation
        return ResponseEntity.ok("List of approved students");
    }

//    @GetMapping("/getGrantZoomClass/{studentId}")
//    public ResponseEntity<?> getGrantZoomClass(@PathVariable Long studentId) {
//        // Placeholder implementation
//        return studentService.getGrantZoomClass(studentId);
//    }
}
