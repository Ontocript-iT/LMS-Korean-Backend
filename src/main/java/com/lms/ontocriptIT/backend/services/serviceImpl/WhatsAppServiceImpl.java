package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.services.centralServices.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class WhatsAppServiceImpl implements WhatsAppService {

    @Value("${whatsapp.teacher.number:+94774647333}") // Default teacher number
    private String teacherWhatsAppNumber;

    @Override
    public void sendNewStudentRegistrationNotification(String studentName, String email, String phoneNumber, String idNumber) {
        try {
            String message = buildRegistrationMessage(studentName, email, phoneNumber, idNumber);
            String whatsappUrl = generateWhatsAppUrl(teacherWhatsAppNumber, message);

            // Log the WhatsApp URL for manual sending or integration
            System.out.println("WhatsApp Notification URL: " + whatsappUrl);

            // In production, you could:
            // 1. Use WhatsApp Business API to send automatically
            // 2. Store in queue for admin to manually send
            // 3. Integrate with third-party WhatsApp gateway

        } catch (Exception e) {
            System.err.println("Failed to generate WhatsApp notification: " + e.getMessage());
        }
    }

    @Override
    public void sendCustomMessage(String phoneNumber, String message) {
        try {
            String whatsappUrl = generateWhatsAppUrl(phoneNumber, message);
            System.out.println("WhatsApp URL: " + whatsappUrl);
        } catch (Exception e) {
            System.err.println("Failed to generate WhatsApp message: " + e.getMessage());
        }
    }

    private String buildRegistrationMessage(String studentName, String email, String phoneNumber, String idNumber) {
        StringBuilder message = new StringBuilder();
        message.append("🎓 *New Student Registration Alert*\n\n");
        message.append("A new student has registered for the LMS:\n\n");
        message.append("📝 *Name:* ").append(studentName).append("\n");
        message.append("📧 *Email:* ").append(email).append("\n");
        message.append("📱 *Phone:* ").append(phoneNumber).append("\n");
        message.append("🆔 *ID Number:* ").append(idNumber).append("\n\n");
        message.append("⏰ Please review and approve this registration in the admin panel.\n\n");
        message.append("_This is an automated notification from LMS_");

        return message.toString();
    }

    private String generateWhatsAppUrl(String phoneNumber, String message) {
        try {
            // Remove any spaces or special characters from phone number
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");

            // Ensure number starts with country code
            if (!cleanNumber.startsWith("+")) {
                cleanNumber = "+" + cleanNumber;
            }

            // URL encode the message
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString());

            // Generate WhatsApp URL
            return "https://wa.me/" + cleanNumber.substring(1) + "?text=" + encodedMessage;

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode WhatsApp message", e);
        }
    }
}
