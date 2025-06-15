package com.example.beusuarios.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.base-url:http://localhost:4200}") // Default frontend URL
    private String frontendBaseUrl;

    public void sendConfirmationEmail(String toEmail, String confirmationToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Confirm Your Account");
            // In a real app, use a proper email template engine
            message.setText("Thank you for registering! Please click the link below to confirm your email address:\n"
                          + frontendBaseUrl + "/auth/confirm-email?token=" + confirmationToken);
            mailSender.send(message);
            System.out.println("Confirmation email sent to " + toEmail); // Log for dev
        } catch (Exception e) {
            // Log the exception in a real app
            System.err.println("Error sending confirmation email to " + toEmail + ": " + e.getMessage());
            // Consider how to handle this failure, e.g., retry mechanism or user notification
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
         try {
             SimpleMailMessage message = new SimpleMailMessage();
             message.setTo(toEmail);
             message.setSubject("Password Reset Request");
             message.setText("You requested a password reset. Click the link below to reset your password:\n"
                           + frontendBaseUrl + "/auth/reset-password-form?token=" + resetToken + "\n" // Assuming a form page
                           + "If you did not request this, please ignore this email. This link will expire in 15 minutes.");
             mailSender.send(message);
             System.out.println("Password reset email sent to " + toEmail); // Log for dev
         } catch (Exception e) {
             System.err.println("Error sending password reset email to " + toEmail + ": " + e.getMessage());
         }
     }
}
