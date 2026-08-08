package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.services.EmailService;

import lombok.*;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;

    @Value("${MAIL_USERNAME}")
    private String from;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(User user, String token) {

        
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Password Recovery");

        message.setText(
                "Hola " + user.getFirstName() + ",\n\n" +
                "Recibimos una solicitud para restablecer tu contraseña.\n\n" +
                "Ingresa al siguiente enlace para crear una nueva contraseña:\n\n" +
                resetLink + "\n\n" +
                "Este enlace caducará en 30 minutos.\n\n" +
                "Si no solicitaste restablecer tu contraseña, puedes ignorar este correo."
        );

        mailSender.send(message);
    }
    
}
