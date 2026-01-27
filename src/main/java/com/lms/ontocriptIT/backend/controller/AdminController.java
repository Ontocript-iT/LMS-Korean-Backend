package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.dtos.ApproveRegistrationDTO;
import com.lms.ontocriptIT.backend.services.centralServices.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/registrations/pending")
    public ResponseEntity<?> getPendingRegistrations(@PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return adminService.getPendingRegistrations(pageable);
    }

    @PostMapping("/registrations/approve")
    public ResponseEntity<?> approveRegistration(@RequestBody ApproveRegistrationDTO dto) {
        return adminService.approveRegistration(dto);
    }

    @PostMapping("/registrations/reject/{requestId}")
    public ResponseEntity<?> rejectRegistration(@PathVariable Long requestId) {
        return adminService.rejectRegistration(requestId);
    }

    @GetMapping("/registrations/approvedStudents")
    public ResponseEntity<?> getApprovedStudents(
            @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return adminService.getApprovedStudents(pageable);
    }

    @GetMapping("/getStudentById/{studentId}")
    public ResponseEntity<?> getStudentById(@PathVariable String studentId) {
        return adminService.getStudentById(studentId);
    }

    @GetMapping("/registrations/searchApprovedStudentById/{studentId}")
    public ResponseEntity<?> searchApprovedStudentById(@PathVariable String studentId){
        return adminService.searchApprovedStudentById(studentId);

    }

}
