package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.dtos.OtpDTO;
import com.lms.ontocriptIT.backend.services.centralServices.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestBody OtpDTO.Request request) {
        return  otpService.generateAndSendOtp(request.getMobileNumber());
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpDTO.Verify request) {
        return otpService.validateOtp(request.getMobileNumber(), request.getOtp());

    }
}