package com.lms.ontocriptIT.backend.auth;

// You can put this in a separate file or as an inner static class
public class ResetPasswordRequest {
    private String identifier; // email or username
    private String otp;
    private String newPassword;

    // Getters and Setters
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}