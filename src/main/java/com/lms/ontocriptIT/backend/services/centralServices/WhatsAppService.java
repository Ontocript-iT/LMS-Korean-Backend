package com.lms.ontocriptIT.backend.services.centralServices;

public interface WhatsAppService {
    void sendNewStudentRegistrationNotification(String studentName, String email, String phoneNumber, String idNumber);
    void sendCustomMessage(String phoneNumber, String message);
}
