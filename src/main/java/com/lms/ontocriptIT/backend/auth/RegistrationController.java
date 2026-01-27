package com.lms.ontocriptIT.backend.auth;


import com.lms.ontocriptIT.backend.dtos.StudentRegistrationDTO;
import com.lms.ontocriptIT.backend.services.centralServices.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RegistrationController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<?> registerStudent(@Valid @RequestBody StudentRegistrationDTO registrationDTO) {
        return authService.registerStudent(registrationDTO);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();

        FieldError error = ex.getBindingResult().getFieldError();
        String errorMessage = error != null ? error.getDefaultMessage() : "Validation error";

        if (error != null && "idNumber".equals(error.getField())) {
            errorMessage = "ජාතික හැඳුනුම්පත් අංකය වැරදියි (සංඛ්‍යා 9ක් සහ V අකුර හෝ ඉලක්කම් 12ක් විය යුතුය).";
        }

        response.put("message", errorMessage);
        response.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.badRequest().body(response);
    }
}
