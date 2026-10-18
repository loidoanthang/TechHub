package com.techhub.service.auth;

public interface EmailService {

    void sendVerificationEmail(String to, String token);

    void sendPasswordResetEmail(String to, String token);
}
