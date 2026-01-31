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
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            String resetLink = baseUrl + "/reset-password?token=" + resetToken;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "LMS - OntocriptIT");
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - LMS");
            helper.setReplyTo("support@ontocriptit.com");

            String htmlBody = buildPasswordResetEmailBody(resetLink);
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

//    private String buildAccountCreationEmailBody(String studentId, String temporaryPassword, String resetLink) {
//        return "<!DOCTYPE html>" +
//                "<html>" +
//                "<head>" +
//                "<meta charset='UTF-8'>" +
//                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
//                "<style>" +
//                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
//                ".container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4; }" +
//                ".email-content { background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
//                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; margin: -30px -30px 20px -30px; }" +
//                ".header h1 { margin: 0; font-size: 24px; }" +
//                ".credentials-box { background-color: #f8f9fa; border-left: 4px solid #667eea; padding: 15px; margin: 20px 0; border-radius: 5px; }" +
//                ".credentials-box p { margin: 8px 0; font-size: 14px; }" +
//                ".credentials-box strong { color: #667eea; font-size: 16px; }" +
//                ".button { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 25px; margin: 20px 0; font-weight: bold; }" +
//                ".button:hover { opacity: 0.9; }" +
//                ".info-box { background-color: #e7f3ff; border-left: 4px solid #2196F3; padding: 15px; margin: 15px 0; border-radius: 5px; }" +
//                ".warning-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 15px 0; border-radius: 5px; }" +
//                ".footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #777; font-size: 12px; }" +
//                "ul { padding-left: 20px; }" +
//                "ul li { margin: 8px 0; }" +
//                "</style>" +
//                "</head>" +
//                "<body>" +
//                "<div class='container'>" +
//                "<div class='email-content'>" +
//                "<div class='header'>" +
//                "<h1>🎓 Welcome to LMS Platform</h1>" +
//                "</div>" +
//
//                "<p>Dear Student,</p>" +
//
//                "<p>Congratulations! Your account has been successfully created in our <strong>Learning Management System (LMS)</strong>.</p>" +
//
//                "<div class='credentials-box'>" +
//                "<h3 style='margin-top: 0; color: #667eea;'>📋 Your Login Credentials</h3>" +
//                "<p><strong>Student ID:</strong> " + studentId + "</p>" +
//                "<p><strong>Temporary Password:</strong> " + temporaryPassword + "</p>" +
//                "</div>" +
//
//                "<div class='info-box'>" +
//                "<p><strong>💡 Login Options:</strong></p>" +
//                "<p>You can login using any of the following as your username:</p>" +
//                "<ul>" +
//                "<li>Your Student ID</li>" +
//                "<li>Your Email Address</li>" +
//                "<li>Your Registered Phone Number</li>" +
//                "</ul>" +
//                "</div>" +
//
//                "<div class='warning-box'>" +
//                "<p><strong>🔐 Security Recommendation:</strong></p>" +
//                "<p>For your account security, we strongly recommend changing your temporary password immediately.</p>" +
//                "</div>" +
//
//                "<div style='text-align: center;'>" +
//                "<a href='" + resetLink + "' class='button'>Reset Password Now</a>" +
//                "</div>" +
//
//                "<p style='text-align: center; color: #777; font-size: 13px;'>Or copy this link: <br>" +
//                "<span style='word-break: break-all;'>" + resetLink + "</span></p>" +
//
//                "<div class='info-box'>" +
//                "<p><strong>⏰ Important Notes:</strong></p>" +
//                "<ul>" +
//                "<li>This password reset link will expire in <strong>7 days</strong></li>" +
//                "<li>If you don't reset your password now, you can still login with the temporary password</li>" +
//                "<li>You can change your password anytime from your account settings</li>" +
//                "</ul>" +
//                "</div>" +
//
//                "<p>If you have any questions or need assistance, please don't hesitate to contact our support team.</p>" +
//
//                "<p>Best regards,<br>" +
//                "<strong>LMS Administration Team</strong><br>" +
//                "OntocriptIT</p>" +
//
//                "<div class='footer'>" +
//                "<p>This is an automated email. Please do not reply directly to this message.</p>" +
//                "<p>© 2026 OntocriptIT Learning Management System. All rights reserved.</p>" +
//                "</div>" +
//
//                "</div>" +
//                "</div>" +
//                "</body>" +
//                "</html>";
//    }

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
