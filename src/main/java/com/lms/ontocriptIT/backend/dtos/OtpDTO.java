package com.lms.ontocriptIT.backend.dtos;

import lombok.Data;

public class OtpDTO {

    @Data
    public static class Request {
        private String mobileNumber; // Format: 9477xxxxxxx
    }

    @Data
    public static class Verify {
        private String mobileNumber;
        private String otp;
    }
}