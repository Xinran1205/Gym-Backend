package com.gym.service;

public interface MailService {

    void sendVerificationCode(String toEmail, String code);

    void sendResetLink(String toEmail, String resetLink);
}
