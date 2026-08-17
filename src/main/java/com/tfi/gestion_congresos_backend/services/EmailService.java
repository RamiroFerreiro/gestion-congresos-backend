package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.entities.User;

public interface EmailService {

    void sendPasswordResetEmail(User user, String token);

}