package com.tfi.gestion_congresos_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfi.gestion_congresos_backend.entities.PasswordResetToken;
import com.tfi.gestion_congresos_backend.entities.User;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByUser(User user);

    Optional<PasswordResetToken> findByToken(String token);
}
