package com.lms.ontocriptIT.backend.controller;

import com.lms.ontocriptIT.backend.services.SmsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmsController {

    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    // Example URL: http://localhost:8080/send-sms?mobile=94771234567&msg=HelloFromSpringBoot
    @GetMapping("/send-sms")
    public String sendSms(@RequestParam String mobile, @RequestParam String msg) {
        return smsService.sendSms(mobile, msg);
    }
}