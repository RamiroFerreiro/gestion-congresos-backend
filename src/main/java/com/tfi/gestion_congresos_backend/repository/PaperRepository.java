
package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.Paper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {

    /// Busca todos los trabajos asignados al ID de un evaluador
    List<Paper> findByUserReviewer_UserId(Long reviewerId);

}