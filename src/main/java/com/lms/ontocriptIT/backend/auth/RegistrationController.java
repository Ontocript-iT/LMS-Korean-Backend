package com.lms.ontocriptIT.backend.auth;


import com.lms.ontocriptIT.backend.dtos.StudentRegistrationDTO;
import com.lms.ontocriptIT.backend.services.centralServices.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
