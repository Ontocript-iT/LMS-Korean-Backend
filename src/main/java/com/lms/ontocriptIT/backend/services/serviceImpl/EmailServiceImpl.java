package com.lms.ontocriptIT.backend.services.serviceImpl;


import com.lms.ontocriptIT.backend.services.centralServices.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.sender.email}")
    private String senderEmail;

    @Override
    public void sendAccountCreationEmail(String toEmail, String studentId,
                                         String temporaryPassword, String resetToken) {
        try {
            String resetLink = "https://www.kandyepstopik.lk" + "/auth/reset-default-password?token=" + resetToken;
//            String resetLink = "http://localhost:3000" + "/auth/reset-default-password?token=" + resetToken;

            System.out.println("Preparing to send account creation email to " + resetLink);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "LMS - OntocriptIT");
            helper.setTo(toEmail);
            helper.setSubject("Welcome to LMS - Your Account Has Been Created");
            helper.setReplyTo("support@ontocriptit.com");

            String htmlBody = buildAccountCreationEmailBody(studentId, temporaryPassword, resetLink);
            helper.setText(htmlBody, true); // true = HTML content

            mailSender.send(message);
            System.out.println("Account creation email sent successfully to " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Failed to send account creation email: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding error: " + e.getMessage());
        }
    }



    @Override
    public void sendPasswordResetOtp(String toEmail, String otp) {
        try {
            System.out.println("Sending OTP " + otp + " to " + toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "LMS - OntocriptIT");
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - OTP");

            // Build the HTML body with the OTP
            String htmlBody = buildOtpEmailBody(otp);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            System.out.println("OTP email sent successfully to " + toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("Failed to send OTP email: " + e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }

    // Helper method to create a nice HTML email
    private String buildOtpEmailBody(String otp) {
        return "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                + "<h2>Password Reset Request</h2>"
                + "<p>You have requested to reset your password. Use the OTP below to proceed:</p>"
                + "<h1 style=\"color: #007bff; letter-spacing: 5px;\">" + otp + "</h1>"
                + "<p>This OTP is valid for <strong>15 minutes</strong>.</p>"
                + "<p>If you did not request this, please ignore this email.</p>"
                + "<br>"
                + "<p>Regards,<br><strong>OntocriptIT Team</strong></p>"
                + "</div>";
    }
    @Override
    public void sendPasswordResetEmail(String toEmail, String studentId,
                                       String temporaryPassword, String resetToken) {
        try {
            String resetLink = "https://www.kandyepstopik.lk" + "/auth/reset-default-password?token=" + resetToken;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "LMS - OntocriptIT");
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - LMS");
            helper.setReplyTo("support@ontocriptit.com");

            String htmlBody = buildPasswordResetEmailBody(studentId,temporaryPassword,resetLink);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            System.out.println("Password reset email sent successfully to " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding error: " + e.getMessage());
        }
    }

    private String buildPasswordResetEmailBody(String studentId, String temporaryPassword, String resetLink) {
        return "<div style=\"font-family: 'Helvetica Neue', Helvetica, Arial, 'Iskoola Pota', 'Nirmala UI', sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;\">"
                + "  <div style=\"background-color: #007bff; padding: 20px; text-align: center; color: #ffffff;\">"
                + "    <h2 style=\"margin: 0;\">Kandy EPS Topik</h2>"
                + "  </div>"
                + "  <div style=\"padding: 30px; background-color: #ffffff;\">"
                + "    <p style=\"font-size: 16px; color: #333333; margin-bottom: 5px;\">Hello / ආයුබෝවන්,</p>"
                + "    <p style=\"font-size: 15px; color: #555555; line-height: 1.5; margin-bottom: 10px;\">"
                + "      Below are your temporary login credentials. Please use the link below to verify your account and set a new password."
                + "    </p>"
                + "    <p style=\"font-size: 15px; color: #555555; line-height: 1.5; margin-bottom: 20px;\">"
                + "      ඔබගේ තාවකාලික පිවිසුම් විස්තර (Login details) පහත දැක්වේ. ඔබගේ ගිණුම තහවුරු කර නව මුරපදයක් (New Password) සකසා ගැනීමට පහත බොත්තම click කරන්න."
                + "    </p>"
                + "    <div style=\"background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; padding: 15px; margin: 20px 0;\">"
                + "      <p style=\"margin: 5px 0; font-size: 15px;\"><strong>Student ID (ශිෂ්‍ය අංකය):</strong> <span style=\"color: #007bff; font-weight: bold;\">" + studentId + "</span></p>"
                + "      <p style=\"margin: 5px 0; font-size: 15px;\"><strong>Temporary Password (තාවකාලික මුරපදය):</strong> <span style=\"font-family: monospace; background-color: #eee; padding: 2px 6px; border-radius: 4px; font-weight: bold;\">" + temporaryPassword + "</span></p>"
                + "    </div>"
                + "    <div style=\"text-align: center; margin-top: 30px;\">"
                + "      <a href=\"" + resetLink + "\" style=\"background-color: #28a745; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 5px; font-weight: bold; font-size: 16px; display: inline-block;\">Verify & Change Password<br><span style=\"font-size:13px; font-weight:normal;\">(ගිණුම තහවුරු කරන්න)</span></a>"
                + "    </div>"
                + "    <p style=\"margin-top: 30px; font-size: 13px; color: #777777; text-align: center;\">"
                + "      If the button doesn't work, copy and paste this link into your browser:<br>"
                + "      (ඉහත බොත්තම ක්‍රියා නොකරයි නම්, පහත සබැඳිය භාවිතා කරන්න)<br>"
                + "      <a href=\"" + resetLink + "\" style=\"color: #007bff; word-break: break-all;\">" + resetLink + "</a>"
                + "    </p>"
                + "  </div>"
                + "  <div style=\"background-color: #f1f1f1; padding: 15px; text-align: center; font-size: 12px; color: #666666;\">"
                + "    &copy; " + java.time.Year.now().getValue() + " Kandy EPS Topik. All rights reserved."
                + "  </div>"
                + "</div>";
    }


    private String buildAccountCreationEmailBody(String studentId, String temporaryPassword, String resetLink) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&family=Noto+Sans+Sinhala:wght@400;700&display=swap');" +
                "    body { font-family: 'Inter', -apple-system, sans-serif; line-height: 1.6; color: #334155; margin: 0; padding: 0; background-color: #f1f5f9; }" +
                "    .container { max-width: 600px; margin: 40px auto; padding: 0 20px; }" +
                "    .email-content { background-color: #ffffff; padding: 40px; border-radius: 24px; border: 1px solid #e2e8f0; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.05); }" +
                "    .header { text-align: center; margin-bottom: 32px; }" +
                "    .header h1 { color: #1e3a8a; font-size: 24px; font-weight: 800; margin: 0; font-family: 'Noto Sans Sinhala', 'Inter', sans-serif; }" +
                "    .sinhala-text { font-family: 'Noto Sans Sinhala', 'Inter', sans-serif; }" +
                "    .credentials-wrapper { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 20px; padding: 25px; margin: 24px 0; }" +
                "    .credential-row { margin: 12px 0; border-bottom: 1px dashed #e2e8f0; padding-bottom: 12px; }" +
                "    .credential-row:last-child { border-bottom: none; padding-bottom: 0; }" +
                "    .label { font-size: 12px; color: #64748b; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 4px; }" +
                "    .value { font-size: 18px; color: #1e3a8a; font-weight: 700; font-family: monospace; }" +
                "    .button-container { text-align: center; margin: 32px 0; }" +
                "    .button { display: inline-block; padding: 16px 40px; background-color: #1e3a8a; color: #ffffff !important; text-decoration: none; border-radius: 12px; font-weight: 700; font-size: 16px; transition: background-color 0.3s; }" +
                "    .hint-box { background-color: #eff6ff; border-radius: 16px; padding: 20px; margin: 20px 0; color: #1e40af; font-size: 14px; }" +
                "    .warning-text { color: #9a3412; background-color: #fff7ed; padding: 12px 16px; border-radius: 10px; font-size: 13px; font-weight: 600; display: inline-block; }" +
                "    .footer { text-align: center; margin-top: 40px; color: #94a3b8; font-size: 12px; border-top: 1px solid #f1f5f9; padding-top: 24px; }" +
                "    .list-item { margin: 8px 0; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "    <div class='email-content'>" +
                "        <div class='header'>" +
                "            <h1 class='sinhala-text'>ආයුබෝවන්! සාදරයෙන් පිළිගනිමු</h1>" +
                "        </div>" +

                "        <p class='sinhala-text' style='font-size: 16px; color: #475569;'>ගරු ශිෂ්‍යයා,</p>" +
                "        <p class='sinhala-text' style='font-size: 15px; color: #475569;'>ඔබේ ගිණුම අපගේ <strong>ඉගෙනුම් කළමනාකරණ පද්ධතිය (LMS)</strong> තුළ සාර්ථකව නිර්මාණය කර ඇත. ඔබගේ පිවිසුම් තොරතුරු පහත දැක්වේ:</p>" +

                "        <div class='credentials-wrapper'>" +
                "            <div class='credential-row'>" +
                "                <span class='label sinhala-text'>ශිෂ්‍ය හැඳුනුම්පත (Student ID)</span>" +
                "                <span class='value'>" + studentId + "</span>" +
                "            </div>" +
                "            <div class='credential-row'>" +
                "                <span class='label sinhala-text'>තාවකාලික මුරපදය (Temp Password)</span>" +
                "                <span class='value'>" + temporaryPassword + "</span>" +
                "            </div>" +
                "        </div>" +

                "        <div class='hint-box'>" +
                "            <p class='sinhala-text' style='margin: 0 0 10px 0;'><strong>💡 ඔබට පිවිසිය හැකි ක්‍රම:</strong></p>" +
                "            <div class='sinhala-text' style='font-size: 13px;'>" +
                "                • ශිෂ්‍ය හැඳුනුම්පත (Student ID)<br>" +
                "                • විද්‍යුත් තැපැල් ලිපිනය (Email)<br>" +
                "                • දුරකථන අංකය (Phone Number)" +
                "            </div>" +
                "        </div>" +

                "        <div style='text-align: center; margin-bottom: 24px;'>" +
                "            <div class='warning-text sinhala-text'>🔐 ආරක්ෂාව සඳහා කරුණාකර මුරපදය වහාම වෙනස් කරන්න.</div>" +
                "        </div>" +

                "        <div class='button-container'>" +
                "            <a href='" + resetLink + "' class='button sinhala-text'>මුරපදය යළි පිහිටුවන්න</a>" +
                "        </div>" +

                "        <div class='sinhala-text' style='font-size: 13px; color: #64748b; margin-top: 30px;'>" +
                "            <p><strong>සැලකිය යුතුයි:</strong></p>" +
                "            <div class='list-item'>• මෙම සබැඳිය <strong>දින 7 කින්</strong> කල් ඉකුත් වනු ඇත.</div>" +
                "            <div class='list-item'>• ඔබට ඕනෑම වේලාවක ගිණුම් සැකසුම් හරහා මුරපදය වෙනස් කළ හැකිය.</div>" +
                "        </div>" +

                "        <p class='sinhala-text' style='font-size: 14px; margin-top: 32px; color: #475569;'>" +
                "            ස්තූතියි,<br>" +
                "            <strong>LMS පරිපාලන කණ්ඩායම</strong><br>" +
                "            <span style='color: #94a3b8; font-size: 12px;'>OntocriptIT</span>" +
                "        </p>" +

                "        <div class='footer sinhala-text'>" +
                "            <p>මෙය ස්වයංක්‍රීය පණිවිඩයකි. කරුණාකර මෙයට පිළිතුරු නොදෙන්න.</p>" +
                "            <p>© 2026 OntocriptIT LMS. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    private String buildPasswordResetEmailBody(String resetLink) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4; }" +
                ".email-content { background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; margin: -30px -30px 20px -30px; }" +
                ".header h1 { margin: 0; font-size: 24px; }" +
                ".button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 25px; margin: 20px 0; font-weight: bold; }" +
                ".button:hover { opacity: 0.9; }" +
                ".warning-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 15px 0; border-radius: 5px; }" +
                ".footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #777; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='email-content'>" +
                "<div class='header'>" +
                "<h1>🔑 Password Reset Request</h1>" +
                "</div>" +

                "<p>Dear Student,</p>" +

                "<p>We received a request to reset your password for your LMS account.</p>" +

                "<div style='text-align: center;'>" +
                "<a href='" + resetLink + "' class='button'>Reset Your Password</a>" +
                "</div>" +

                "<p style='text-align: center; color: #777; font-size: 13px;'>Or copy this link: <br>" +
                "<span style='word-break: break-all;'>" + resetLink + "</span></p>" +

                "<div class='warning-box'>" +
                "<p><strong>⚠️ Important:</strong></p>" +
                "<ul>" +
                "<li>This link will expire in <strong>24 hours</strong></li>" +
                "<li>If you didn't request this password reset, please ignore this email</li>" +
                "<li>Your password will remain unchanged unless you click the link above</li>" +
                "</ul>" +
                "</div>" +

                "<p>If you're having trouble clicking the button, copy and paste the URL into your web browser.</p>" +

                "<p>Best regards,<br>" +
                "<strong>LMS Administration Team</strong><br>" +
                "OntocriptIT</p>" +

                "<div class='footer'>" +
                "<p>This is an automated email. Please do not reply directly to this message.</p>" +
                "<p>© 2026 OntocriptIT Learning Management System. All rights reserved.</p>" +
                "</div>" +

                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
