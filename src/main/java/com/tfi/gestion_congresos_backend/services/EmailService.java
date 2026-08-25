package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.entities.User;

public interface EmailService {

    void sendPasswordResetEmail(User user, String token);

    void sendCurrentEmailChangeVerificationEmail(User user, String token);
    
    void sendNewEmailChangeVerificationEmail(String newEmail, User user, String token);

}