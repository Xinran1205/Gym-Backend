package com.gym.service;

public interface MailService {

//    void sendVerificationEmail(String to, String subject, String text);
    void sendVerificationCode(String toEmail, String code);

    void sendAdminNotification(String adminEmail, String message);
}
