package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.RoleName;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmailWithRole(String email);
    
    @Query("SELECT DISTINCT p FROM Congress c JOIN c.participants p JOIN p.role r WHERE c.congressId = :congressId AND (:role IS NULL OR r.name = :role)")
	List<User> findParticipantsByCongressIdAndRole(@Param("congressId") Long congressId, @Param("role") RoleName role);   
}
