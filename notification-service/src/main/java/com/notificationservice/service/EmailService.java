package com.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Auth Service}")
    private String appName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String username, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(appName + " — Verify Your Email");
            helper.setText(buildOtpEmailHtml(username, otpCode), true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (MessagingException ex) {
            log.error("Failed to send email to {}: {}", toEmail, ex.getMessage());
            throw new RuntimeException("Failed to send verification email", ex);
        }
    }

    private String buildOtpEmailHtml(String username, String otpCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f7; margin: 0; padding: 0; }
                        .container { max-width: 520px; margin: 40px auto; background: #ffffff; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); overflow: hidden; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 32px; text-align: center; }
                        .header h1 { color: #ffffff; margin: 0; font-size: 24px; font-weight: 600; }
                        .body { padding: 32px; }
                        .body p { color: #51545e; font-size: 15px; line-height: 1.6; margin: 0 0 16px; }
                        .otp-box { background: #f4f4f7; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 24px 0; }
                        .otp-code { font-size: 36px; font-weight: 700; color: #333333; letter-spacing: 8px; margin: 0; }
                        .expiry { color: #e74c3c; font-size: 13px; font-weight: 600; }
                        .footer { padding: 20px 32px; background: #f4f4f7; text-align: center; }
                        .footer p { color: #a8aaaf; font-size: 12px; margin: 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                        </div>
                        <div class="body">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>Welcome! To complete your registration, please use the following verification code:</p>
                            <div class="otp-box">
                                <p class="otp-code">%s</p>
                            </div>
                            <p class="expiry">⏱ This code expires in 5 minutes.</p>
                            <p>If you didn't create an account, you can safely ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>© 2026 %s. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(appName, username, otpCode, appName);
    }
}
