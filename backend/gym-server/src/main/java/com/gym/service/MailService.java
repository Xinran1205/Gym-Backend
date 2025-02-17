package com.gym.service;

public interface MailService {
    /**
     * 发送验证码给用户
     */
    void sendVerificationCode(String toEmail, String code);

    /**
     * 发送给管理员的提醒邮件
     */
    void sendAdminNotification(String adminEmail, String message);
}
