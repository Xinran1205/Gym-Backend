//package com.gym.service.impl;
//
//import com.gym.service.MailService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.messaging.MessagingException;
//import org.springframework.stereotype.Service;
//
//
//@Service
//@Slf4j
//public class MailServiceImpl implements MailService {
//
//    @Autowired
//    private JavaMailSender mailSender;
//
//    @Override
//    public void sendVerificationCode(String toEmail, String code) {
//        String subject = "Fitness App - 验证码";
//        String text = "您好，\n\n您的验证码是: " + code + "\n\n请在10分钟内完成验证。";
//        sendMail(toEmail, subject, text);
//    }
//
//    @Override
//    public void sendAdminNotification(String adminEmail, String message) {
//        String subject = "有新用户待审核";
//        String text = "尊敬的管理员，\n\n" + message +
//                "\n请登录后台管理系统进行审核。\n\n此致";
//        sendMail(adminEmail, subject, text);
//    }
//
//    /**
//     * 通用的发送邮件方法
//     */
//    private void sendMail(String to, String subject, String text) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            // true表示需要创建一个multipart message
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//            // 发件人可以与 spring.mail.username 保持一致，也可自定义别名(需邮箱支持)
//            helper.setFrom("你的QQ邮箱@qq.com");
//            helper.setTo(to);
//            helper.setSubject(subject);
//            helper.setText(text, false);
//
//            mailSender.send(message);
//            log.info("邮件发送成功: to={}, subject={}", to, subject);
//        } catch (MessagingException e) {
//            log.error("发送邮件失败: to={}, subject={}, 异常={}", to, subject, e.getMessage());
//        }
//    }
//}
