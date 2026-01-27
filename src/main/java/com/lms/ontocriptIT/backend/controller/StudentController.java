package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.entity.User;
//import com.lms.ontocriptIT.backend.services.centralServices.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {

        HashMap<String,Object> res = new HashMap<>();
        res.put("message", "User profile fetched successfully");
        res.put("firsName", user.getFirstName());
        res.put("lastName", user.getLastName());
        res.put("studentId", user.getStudentId());
        res.put("email", user.getEmail());
        res.put("district", user.getDistrict());
        res.put("status", user.getStatus());
        res.put("phone1", user.getPhoneNumber1());
        res.put("phone2", user.getPhoneNumber2());
        res.put("regDate", user.getCreatedAt());
        res.put("statusCode", HttpStatus.OK.value());

        return ResponseEntity.ok(res);
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
    
}
