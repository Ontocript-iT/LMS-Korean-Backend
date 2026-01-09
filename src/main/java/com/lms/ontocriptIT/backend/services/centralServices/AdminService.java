package com.lms.ontocriptIT.backend.services.centralServices;

import com.lms.ontocriptIT.backend.dtos.ApproveRegistrationDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface AdminService {
    ResponseEntity<?> getPendingRegistrations();
    ResponseEntity<?> approveRegistration(ApproveRegistrationDTO dto);
    ResponseEntity<?> rejectRegistration(Long requestId);

    ResponseEntity<?> getApprovedStudents(Pageable pageable);

    ResponseEntity<?> getStudentById(String studentId);

    ResponseEntity<?> searchApprovedStudentById(String studentId);

}