package com.lms.ontocriptIT.backend.services.centralServices;


public interface EmailService {
    void sendAccountCreationEmail(String toEmail, String studentId, String temporaryPassword, String resetToken);
    void sendPasswordResetEmail(String toEmail, String resetToken);
}