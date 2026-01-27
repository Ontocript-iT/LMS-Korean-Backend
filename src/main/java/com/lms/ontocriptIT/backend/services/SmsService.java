package com.lms.ontocriptIT.backend.services;

import com.lms.ontocriptIT.backend.dtos.TextLkSmsRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class SmsService {

    @Value("${textlk.api.url}")
    private String apiUrl;

    @Value("${textlk.api.key}")
    private String apiKey;

    @Value("${textlk.sender.id}")
    private String senderId;

    private final RestTemplate restTemplate;

    public SmsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String sendSms(String phoneNumber, String message) {
        // 1. Prepare Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 2. Prepare Body
        TextLkSmsRequest requestPayload = TextLkSmsRequest.builder()
                .recipient(phoneNumber)
                .senderId(senderId)
                .type("plain")
                .message(message)
                .build();

        // 3. Create Request Entity
        HttpEntity<TextLkSmsRequest> request = new HttpEntity<>(requestPayload, headers);

        // 4. Send Request
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            return response.getBody(); // Returns the JSON response from Text.lk
        } catch (Exception e) {
            e.printStackTrace();
            return "Error sending SMS: " + e.getMessage();
        }
    }
}