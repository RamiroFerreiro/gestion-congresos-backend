package com.tfi.gestion_congresos_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.RoleName;

public interface CongressParticipantRepository extends JpaRepository<CongressParticipant, Long> {

    // 1. Verifica si un usuario es Administrador activo de un congreso específico
    @Query("""
        SELECT COUNT(p) > 0 
        FROM CongressParticipant p 
        WHERE p.user.userId = :userId 
        AND p.congress.code = :congressCode 
        AND p.role.name = :role 
        AND p.active = true
    """)
    boolean hasRoleInCongress(
        @Param("userId") Long userId, 
        @Param("congressCode") String congressCode, 
        @Param("role") RoleName role
    );

    // 2. Verifica si el usuario objetivo participa (activo) en ese mismo congreso
    @Query("""
        SELECT COUNT(p) > 0 
        FROM CongressParticipant p 
        WHERE p.user.userId = :targetUserId 
        AND p.congress.code = :congressCode 
        AND p.active = true
    """)
    boolean isUserParticipantOfCongress(
        @Param("targetUserId") Long targetUserId, 
        @Param("congressCode") String congressCode
    );

    //Verifica si el usuario objetivo participa (activo) en ese mismo congreso y retorna el usuario
    Optional<CongressParticipant> findByUser_CodeAndCongress_CodeAndActiveTrue(
        String userCode, 
        String congressCode
    );


    // Consulta para el Admin de Congreso: verifica que el solicitante sea Admin de 'congressCode'
    // y trae únicamente los usuarios activos de ese mismo congreso.
    @Query("""
        SELECT DISTINCT p2.user 
        FROM CongressParticipant p1
        JOIN CongressParticipant p2 ON p1.congress.congressId = p2.congress.congressId
        WHERE p1.user.userId = :adminUserId
        AND p1.role.name = :role
        AND p1.congress.code = :congressCode
        AND p1.active = true
        AND p2.active = true
    """)
    List<User> findUsersByCongressAndAdmin(
        @Param("adminUserId") Long adminUserId,
        @Param("congressCode") String congressCode,
        @Param("role") RoleName role
    );

    // Consulta directa para Súper Admin: trae todos los usuarios de ese congreso
    @Query("""
        SELECT DISTINCT p.user 
        FROM CongressParticipant p 
        WHERE p.congress.code = :congressCode 
        AND p.active = true
    """)
    List<User> findUsersByCongressCode(@Param("congressCode") String congressCode);
}