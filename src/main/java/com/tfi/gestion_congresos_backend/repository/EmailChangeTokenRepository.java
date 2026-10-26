package com.tfi.gestion_congresos_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfi.gestion_congresos_backend.entities.EmailChangeToken;
import com.tfi.gestion_congresos_backend.entities.PasswordResetToken;
import com.tfi.gestion_congresos_backend.entities.User;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {
    
    Optional<EmailChangeToken> findByUser(User user);

    Optional<EmailChangeToken> findByToken(String token);
}

