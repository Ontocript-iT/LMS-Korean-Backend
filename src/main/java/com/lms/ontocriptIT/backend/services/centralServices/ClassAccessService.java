package com.lms.ontocriptIT.backend.services.centralServices;

import com.lms.ontocriptIT.backend.dtos.ClassAccessDTO;
import org.springframework.http.ResponseEntity;

public interface ClassAccessService {
    ResponseEntity<?> grantClassAccess(ClassAccessDTO dto, Long adminId);
    ResponseEntity<?> revokeClassAccess(Long accessId, Long adminId, String notes);
    ResponseEntity<?> getStudentClassAccesses(Long studentId);
    ResponseEntity<?> getAllClassAccesses();
    ResponseEntity<?> updateClassAccess(Long accessId, ClassAccessDTO dto, Long adminId);
}
