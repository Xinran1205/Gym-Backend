package com.gym.service.impl;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.gym.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailServiceImpl implements MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async("mailAsyncExecutor")
    @Override
    public void sendVerificationCode(String toEmail, String code) {
        String subject = "Fitness App - Verification Code";
        String text = "Hello,\n\nYour verification code is: " + code + "\n\nPlease complete the verification within 5 minutes.";
        sendMail(toEmail, subject, text);
    }

    /**
     * General method for sending emails
     */
    private void sendMail(String to, String subject, String text) {
        try {
            // 1. 创建邮件对象
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 2. 设置发件人、收件人、标题、内容
            helper.setFrom("yourgmailEmail@gmail.com"); // 需与 spring.mail.username 对应
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            // 3. 发送邮件
            mailSender.send(message);
            log.info("Email sent successfully: to={}, subject={}", to, subject);

        } catch (MessagingException e) {
            // 只捕获 javax.mail.MessagingException 即可
            log.error("Failed to send email: to={}, subject={}, error={}", to, subject, e.getMessage());
            // 若有需要，也可以抛出 RuntimeException 或做其他处理
        }
    }


    @Async("mailAsyncExecutor")
    @Override
    public void sendResetLink(String toEmail, String resetLink) {
        String subject = "Fitness App - Password Reset Link";
        String text = "Hello,\n\nPlease click the following link to reset your password:\n" +
                resetLink + "\n\nThe link is valid for 5 minutes.\n" +
                "If you did not request a password reset, please ignore this email.";
        sendMail(toEmail, subject, text);
    }


}

// 废弃
//    @Override
//    public void sendAdminNotification(String adminEmail, String message) {
//        String subject = "New User Pending Review";
//        String text = "Dear Administrator,\n\n" + message +
//                "\nPlease log in to the admin system to review the new user.\n\nBest regards";
//        sendMail(adminEmail, subject, text);
//    }
