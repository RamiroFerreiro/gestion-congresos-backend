package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    @Query("""
            SELECT e FROM Evaluation e
            JOIN FETCH e.paper p
            WHERE p.congress.code = :congressCode
              AND p.userReviewer.code = :reviewerCode
            ORDER BY e.evaluationDate DESC, e.evaluationId DESC
            """)
    List<Evaluation> findAssignedReviewerEvaluations(
            @Param("congressCode") String congressCode,
            @Param("reviewerCode") String reviewerCode);

    // Te trae las evaluaciones de un paper ordenadas por fecha descendente
    List<Evaluation> findByPaper_CodeOrderByEvaluationDateDesc(String paperCode);

    List<Evaluation> findByPaper_Congress_CodeAndIsReleasedFalse(String congressCode);
    boolean existsByPaper_CodeAndEvaluatedVersion(String paperCode, String evaluatedVersion);
    
    /// Verifica si existe una evaluación pendiente de liberar para una versión específica
    boolean existsByPaper_CodeAndEvaluatedVersionAndIsReleasedFalse(
            String paperCode,
            String evaluatedVersion
    );
}
