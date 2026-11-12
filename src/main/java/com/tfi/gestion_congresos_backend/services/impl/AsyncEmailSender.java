
package com.tfi.gestion_congresos_backend.services.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailSender {

    private final JavaMailSender mailSender;

    @Value("${MAIL_USERNAME}")
    private String from;

    @Async
    public void sendTextEmail(String to, String subject, String text) {

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);

        } catch (MailException e) {
            log.error("No se pudo enviar el email con asunto '{}'.", subject, e);
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String html) {

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {
            log.error("No se pudo enviar el email HTML con asunto '{}'.", subject, e);
        }
    }
}
