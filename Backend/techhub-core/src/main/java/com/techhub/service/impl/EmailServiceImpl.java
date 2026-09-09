package com.techhub.service.impl;

import com.techhub.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Verify your email");
            message.setText(
                    """
                    Welcome to TechHub.
                                    
                    Your verification code is: %s
                                    
                    Please enter this code in the app to verify your account.
                    """.formatted(token)
            );
            mailSender.send(message);
            log.info("Verification email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Reset your password");
            message.setText(
                    """
                    We received a request to reset your password.
        
                    Your password reset code is: %s
        
                    If you did not request this, please ignore this email.
                    """.formatted(token)
            );
            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage(), e);
        }
    }

}
