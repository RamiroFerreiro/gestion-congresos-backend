package com.tfi.gestion_congresos_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.enums.RoleName;

public interface CongressRepository extends JpaRepository<Congress, Long> {
	
	/// Obtener todos los congresos con sus participantes asociados:
	@Query("SELECT DISTINCT c FROM Congress c LEFT JOIN FETCH c.participants p LEFT JOIN FETCH p.role LEFT JOIN FETCH c.thematicAreas")
    List<Congress> findAllCongressesWithParticipants();
	
	/// Obtener los congresos activos/desactivados/todos con sus participantes asociados:
	@Query("SELECT DISTINCT c FROM Congress c LEFT JOIN FETCH c.participants p LEFT JOIN FETCH p.role LEFT JOIN FETCH c.thematicAreas WHERE c.enabled = :enabled")
    List<Congress> findAllCongressesByEnabledWithParticipants(@Param("enabled") boolean enabled);
	
	/// Obtener un congreso con sus participantes asociados por ID:
	@Query("SELECT DISTINCT c FROM Congress c LEFT JOIN FETCH c.participants p LEFT JOIN FETCH p.role LEFT JOIN FETCH c.thematicAreas WHERE c.congressId = :congressId")
    Optional<Congress> findByCongressId(@Param("congressId") Long congressId);
	
	/// Determinar si existe un usuario por su ID con determinado rol en un congreso específico:
	@Query("SELECT COUNT(p) > 0 FROM Congress c JOIN c.participants p JOIN p.role r WHERE c.congressId = :congressId AND p.userId = :userId AND r.name = :role")
	boolean existsByCongressIdAndUserIdAndRole(Long congressId, Long userId, RoleName role);
	
	/// Determinar si existe un participante específico en un congreso determinado:
	boolean existsByCongressIdAndParticipantsUserId(Long congressId, Long userId);
}
