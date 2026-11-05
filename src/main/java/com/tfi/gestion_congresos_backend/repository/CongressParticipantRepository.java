package com.tfi.gestion_congresos_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.enums.RoleName;

public interface CongressParticipantRepository extends JpaRepository<CongressParticipant, Long> {

    /// Verificar si un usuario activo tiene un rol en un congreso:
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

    /// Verificar si un usuario participa activamente en un congreso:
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

    /// Obtener la participación activa de un usuario en un congreso:
    @Query("""
        SELECT p 
        FROM CongressParticipant p 
        WHERE p.user.code = :targetUserCode 
        AND p.congress.code = :congressCode
        AND p.active = true
    """)
    
    Optional<CongressParticipant> findActiveParticipantByUserCodeAndCongressCode(

        @Param("targetUserCode") String targetUserCode,
        @Param("congressCode") String congressCode
    );
    
    /// Obtener los participantes activos de un congreso, opcionalmente filtrados por rol:
    @Query("""
	    SELECT p
	    FROM CongressParticipant p
	    JOIN FETCH p.user
	    JOIN FETCH p.role
	    WHERE p.congress.code = :congressCode
	    AND p.active = true
	    AND (:role IS NULL OR p.role.name = :role)
	""")
	List<CongressParticipant> findParticipantsByCongressCodeAndRole(
	        @Param("congressCode") String congressCode,
	        @Param("role") RoleName role
	);
    
    /// Obtener un participante de un congreso por su código:
    Optional<CongressParticipant> findByUserCodeAndCongressCode(
	    String userCode,
	    String congressCode
	);
    
    /// Contar administradores activos de un congreso:
    @Query("""
        SELECT COUNT(p)
        FROM CongressParticipant p
        WHERE p.congress.code = :congressCode
        AND p.role.name = :role
        AND p.active = true
    """)
    long countActiveParticipantsByCongressCodeAndRole(
            @Param("congressCode") String congressCode,
            @Param("role") RoleName role
    );
}