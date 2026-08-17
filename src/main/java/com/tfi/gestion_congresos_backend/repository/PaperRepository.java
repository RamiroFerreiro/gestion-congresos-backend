
package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.Paper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {

    /// Busca todos los trabajos asignados al ID de un evaluador
    List<Paper> findByUserReviewer_UserId(Long reviewerId);
    
    /// Busca todos los papers de un congreso por su ID:
   @Query("SELECT DISTINCT p FROM Paper p LEFT JOIN FETCH p.authors pa LEFT JOIN FETCH pa.author LEFT JOIN FETCH p.congress c LEFT JOIN FETCH p.userReviewer WHERE c.congressId = :congressId ORDER BY pa.authorOrder ASC")
    List<Paper> findByCongressIdWithDetails(@Param("congressId") Long congressId);

}