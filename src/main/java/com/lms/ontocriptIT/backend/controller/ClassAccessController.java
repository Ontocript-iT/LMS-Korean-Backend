package com.lms.ontocriptIT.backend.controller;


import com.lms.ontocriptIT.backend.dtos.ClassAccessDTO;
import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.services.centralServices.ClassAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/class-access")
@RequiredArgsConstructor
public class ClassAccessController {

    private final ClassAccessService classAccessService;

    @PostMapping("/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> grantClassAccess(
            @Valid @RequestBody ClassAccessDTO dto,
            @AuthenticationPrincipal User admin) {
        return classAccessService.grantClassAccess(dto, admin.getId());
    }

    @PutMapping("/revoke/{accessId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeClassAccess(
            @PathVariable Long accessId,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal User admin) {
        return classAccessService.revokeClassAccess(accessId, admin.getId(), notes);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> getStudentClassAccesses(@PathVariable Long studentId) {
        return classAccessService.getStudentClassAccesses(studentId);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllClassAccesses() {
        return classAccessService.getAllClassAccesses();
    }

    @PutMapping("/update/{accessId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateClassAccess(
            @PathVariable Long accessId,
            @Valid @RequestBody ClassAccessDTO dto,
            @AuthenticationPrincipal User admin) {
        return classAccessService.updateClassAccess(accessId, dto, admin.getId());
    }

//    @PostMapping("/bulkAccess")
}
