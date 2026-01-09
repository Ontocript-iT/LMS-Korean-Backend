package com.lms.ontocriptIT.backend.services.centralServices;


import com.lms.ontocriptIT.backend.auth.LoginRequestDTO;
import com.lms.ontocriptIT.backend.auth.LogoutRequestDTO;
import com.lms.ontocriptIT.backend.auth.ResetPasswordDTO;
import com.lms.ontocriptIT.backend.dtos.StudentRegistrationDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> registerStudent(StudentRegistrationDTO dto);
    ResponseEntity<?> login(LoginRequestDTO dto, HttpServletRequest request);
    ResponseEntity<?> resetPassword(ResetPasswordDTO dto);
    ResponseEntity<?> initiatePasswordReset(String identifier);
}