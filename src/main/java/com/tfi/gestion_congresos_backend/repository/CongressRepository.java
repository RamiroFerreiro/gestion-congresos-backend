package com.tfi.gestion_congresos_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfi.gestion_congresos_backend.entities.Congress;

public interface CongressRepository extends JpaRepository<Congress, Long> {
	
	/// Obtener todos los congresos con sus áreas temáticas asociadas:
    @Query("SELECT DISTINCT c FROM Congress c LEFT JOIN FETCH c.thematicAreas")
    List<Congress> findAllWithThematicAreas();

    /// Obtener los congresos por estado de habilitación con sus áreas temáticas asociadas:
    @Query("SELECT DISTINCT c FROM Congress c LEFT JOIN FETCH c.thematicAreas WHERE c.enabled = :enabled")
    List<Congress> findAllByEnabledWithThematicAreas(@Param("enabled") boolean enabled);

    /// Obtener un congreso por ID con sus áreas temáticas asociadas:
    @Query("SELECT DISTINCT c FROM Congress c LEFT JOIN FETCH c.thematicAreas WHERE c.congressId = :congressId")
    Optional<Congress> findByCongressIdWithThematicAreas(@Param("congressId") Long congressId);

    /// Obtener un congreso por código con sus áreas temáticas asociadas:
    @Query("SELECT DISTINCT c FROM Congress c LEFT JOIN FETCH c.thematicAreas WHERE c.code = :code")
    Optional<Congress> findByCodeWithThematicAreas(@Param("code") String code);

    /// Determinar si existe un congreso por su código:
    boolean existsByCode(String code);
}
