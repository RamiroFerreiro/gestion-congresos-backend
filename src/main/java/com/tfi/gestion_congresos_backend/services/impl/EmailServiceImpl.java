package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.services.EmailService;

import lombok.*;

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

        String resetLink = buildLink("/reset-password", token);

        String text = "Hola " + user.getFirstName() + ",\n\n" +
                "Recibimos una solicitud para restablecer tu contraseña.\n\n" +
                "Ingresa al siguiente enlace para crear una nueva contraseña:\n\n" +
                resetLink + "\n\n" +
                TOKEN_EXPIRATION_MESSAGE + "\n\n" +
                "Si no solicitaste restablecer tu contraseña, puedes ignorar este correo.";

        sendEmail(user.getEmail(), "Password Recovery", text);
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

        String loginLink = frontendUrl + "/login";

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
}