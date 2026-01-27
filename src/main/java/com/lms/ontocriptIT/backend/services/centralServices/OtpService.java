package com.lms.ontocriptIT.backend.services.centralServices;

import org.springframework.http.ResponseEntity;

public interface OtpService {
    ResponseEntity<?> generateAndSendOtp(String mobileNumber);

    ResponseEntity<?> validateOtp(String mobileNumber, String otp);
}
