package com.tfi.gestion_congresos_backend.services.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.services.EmailService;

import lombok.*;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String TOKEN_EXPIRATION_MESSAGE = "Este enlace caducará en 30 minutos.";

    private final JavaMailSender mailSender;

    @Value("${MAIL_USERNAME}")
    private String from;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(User user, String token) {

        String resetLink = buildLink("/auth/reset-password", token);

        String html = loadEmailTemplate(
                "email_templates/auth/c4t-password-recovery.html"
        )
                .replace("{{nombre_usuario}}", user.getFirstName())
                .replace("{{email_usuario}}", user.getEmail())
                .replace("{{tiempo_expiracion}}", "30 minutos")
                .replace("{{url_recuperacion}}", resetLink);

        sendHtmlEmail(
                user.getEmail(),
                "C4T · Recuperación de contraseña",
                html
        );
    }

    @Override
    public void sendCurrentEmailChangeVerificationEmail(User user, String token) {

        String verificationLink = buildLink("/confirm-email-change", token);

        String text = "Hola " + user.getFirstName() + ",\n\n" +
                "Recibimos una solicitud para cambiar el email de tu cuenta.\n\n" +
                "Para continuar con el cambio, confirma que tienes acceso a este correo:\n\n" +
                verificationLink + "\n\n" +
                TOKEN_EXPIRATION_MESSAGE + "\n\n" +
                "Si no solicitaste este cambio, puedes ignorar este correo.";

        sendEmail(user.getEmail(), "Confirmación de cambio de email", text);
    }

    @Override
    public void sendNewEmailChangeVerificationEmail(String newEmail, User user, String token) {

        String verificationLink = buildLink("/confirm-new-email", token);

        String text = "Hola " + user.getFirstName() + ",\n\n" +
                "Se ha solicitado asociar este correo a tu cuenta.\n\n" +
                "Confirma el cambio ingresando al siguiente enlace:\n\n" +
                verificationLink + "\n\n" +
                TOKEN_EXPIRATION_MESSAGE;

        sendEmail(newEmail, "Confirmación de nuevo email", text);
    }

    @Override
    public void sendTemporaryPasswordEmail(User user, String temporaryPassword) {

        String loginLink = frontendUrl + "/auth/login";

        String text = "Hola " + user.getFirstName() + ",\n\n" +
                "Un administrador ha creado tu cuenta en la plataforma.\n\n" +
                "Tus credenciales de acceso son las siguientes:\n" +
                "Email: " + user.getEmail() + "\n" +
                "Contraseña provisoria: " + temporaryPassword + "\n\n" +
                "Ingresa al siguiente enlace para iniciar sesión:\n" +
                loginLink + "\n\n" +
                "Por razones de seguridad, se te solicitará cambiar esta contraseña en tu primer ingreso.";

        sendEmail(user.getEmail(), "Bienvenido - Acceso a tu cuenta", text);
    }

    private String buildLink(String path, String token) {
        return frontendUrl + path + "?token=" + token;
    }

    @Async
    protected void sendEmail(String to, String subject, String text) {

        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    private String loadEmailTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);

            try (InputStream inputStream = resource.getInputStream()) {
                return StreamUtils.copyToString(
                        inputStream,
                        StandardCharsets.UTF_8
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "No se pudo cargar la plantilla de email: " + path, e
            );
        }
    }

    @Async
    protected void sendHtmlEmail(String to, String subject, String html) {
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

        } catch (MessagingException e) {
            throw new RuntimeException(
                    "No se pudo enviar el email HTML", e
            );
        }
    }
}