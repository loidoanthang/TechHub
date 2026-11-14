package com.techhub.modules.auth.service;

public interface EmailService {

    void sendVerificationEmail(String to, String token);

    void sendPasswordResetEmail(String to, String token);
}
