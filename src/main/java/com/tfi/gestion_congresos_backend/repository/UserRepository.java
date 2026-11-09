package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    
    boolean existsByCode(String code);

    Optional<User> findByEmail(String email);
    
    Optional<User> findByCode(String code);
    
    long countBySuperAdminTrueAndEnabledTrue();
}
