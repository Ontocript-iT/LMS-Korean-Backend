package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.services.SmsService;
import com.lms.ontocriptIT.backend.services.centralServices.OtpService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final SmsService smsService;

    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    private static final int OTP_VALIDITY_MINUTES = 5;

    @Data
    @AllArgsConstructor
    private static class OtpData {
        private String otp;
        private LocalDateTime expiryTime;
    }

    @Scheduled(fixedRate = 60000) // 60000ms = 1 minute
    public void removeExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();

        // Remove entries where the expiry time is before NOW
        otpStorage.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().getExpiryTime())
        );
    }

    @Override
    public ResponseEntity<?> generateAndSendOtp(String mobileNumber) {
        String otp = String.valueOf(new SecureRandom().nextInt(900000) + 100000);

        OtpData otpData = new OtpData(otp, LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        otpStorage.put(mobileNumber, otpData);

        String message = "Your OTP for LMS Login is: " + otp + ". Valid for 5 minutes.";

        // Run in background thread to not block response
        new Thread(() -> smsService.sendSms(mobileNumber, message)).start();

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully",
                "status", HttpStatus.OK.value()
        ));
    }


    @Override
    public ResponseEntity<?> validateOtp(String mobileNumber, String inputOtp) {

        if (!otpStorage.containsKey(mobileNumber)) {
            return ResponseEntity.status(400).body(Map.of(
                    "message", "Invalid or Expired OTP",
                    "status", HttpStatus.UNAUTHORIZED.value()
            ));
        }

        OtpData data = otpStorage.get(mobileNumber);

        if (LocalDateTime.now().isAfter(data.getExpiryTime())) {
            otpStorage.remove(mobileNumber); // Clean up
            return ResponseEntity.status(400).body(Map.of(
                    "message", "Invalid or Expired OTP",
                    "status", HttpStatus.UNAUTHORIZED.value()
            ));
        }

        if (data.getOtp().equals(inputOtp)) {
            otpStorage.remove(mobileNumber); // Security: Remove OTP after successful use
            return ResponseEntity.ok(Map.of(
                    "message", "OTP Verified Successfully",
                    "status", HttpStatus.OK.value()
            ));
        }

        return ResponseEntity.status(400).body(Map.of(
                "message", "Invalid or Expired OTP",
                "status", HttpStatus.UNAUTHORIZED.value()
        ));
    }
}
