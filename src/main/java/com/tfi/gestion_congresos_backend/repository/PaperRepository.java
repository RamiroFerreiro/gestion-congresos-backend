
package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.repository.projection.AcademicMetricsProjection;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {

    /// Busca todos los papers de un congreso por su ID:
   @Query("SELECT DISTINCT p FROM Paper p LEFT JOIN FETCH p.authors pa LEFT JOIN FETCH pa.author LEFT JOIN FETCH p.congress c LEFT JOIN FETCH p.userReviewer WHERE c.congressId = :congressId ORDER BY pa.authorOrder ASC")
    List<Paper> findByCongressIdWithDetails(@Param("congressId") Long congressId);

    boolean existsByCode(String code);

    Optional<Paper> findByCode(String code);
    
    /// Busca los trabajos asignados a un evaluador dentro de un congreso
    @EntityGraph(attributePaths = "congress")
    List<Paper> findByCongress_CodeAndUserReviewer_Code(String congressCode, String reviewerCode);
    
    /// Busca trabajos pendientes de reasignación porque su evaluador ya no está habilitado para evaluar
    @EntityGraph(attributePaths = {"congress", "userReviewer"})
    @Query("""
        SELECT p
        FROM Paper p
        WHERE p.congress.code = :congressCode
          AND p.userReviewer IS NOT NULL
          AND p.status IN (
              com.tfi.gestion_congresos_backend.enums.PaperStatus.UNDER_EVALUATION,
              com.tfi.gestion_congresos_backend.enums.PaperStatus.NEEDS_REVISION
          )
          AND NOT EXISTS (
              SELECT cp.idCongressParticipant
              FROM CongressParticipant cp
              WHERE cp.user = p.userReviewer
                AND cp.congress = p.congress
                AND cp.active = true
                AND cp.user.enabled = true
                AND cp.role.name = com.tfi.gestion_congresos_backend.enums.RoleName.EVALUATOR
          )
          AND NOT EXISTS (
              SELECT e
              FROM Evaluation e
              WHERE e.paper = p
                AND e.evaluatedVersion = p.version
                AND e.isReleased = false
          )
        """)
    List<Paper> findPapersPendingReassignment(
            @Param("congressCode") String congressCode
    );
    
    /// Cuenta los trabajos pendientes de evaluación agrupados por evaluador
    @Query("""
        SELECT p.userReviewer.code, COUNT(p)
        FROM Paper p
        WHERE p.congress.code = :congressCode
          AND p.userReviewer IS NOT NULL
          AND p.status IN (
              com.tfi.gestion_congresos_backend.enums.PaperStatus.UNDER_EVALUATION,
              com.tfi.gestion_congresos_backend.enums.PaperStatus.NEEDS_REVISION
          )
          AND NOT EXISTS (
              SELECT e
              FROM Evaluation e
              WHERE e.paper = p
                AND e.evaluatedVersion = p.version
                AND e.isReleased = false
          )
        GROUP BY p.userReviewer.code
        """)
    List<Object[]> countPendingPapersByReviewer(
            @Param("congressCode") String congressCode
    );

    @Query("""
        SELECT 
            COUNT(p) AS totalSubmissions,
            SUM(CASE WHEN p.userReviewer IS NULL THEN 1 ELSE 0 END) AS unassignedSubmissions,
            SUM(CASE WHEN p.status = 'UNDER_EVALUATION' THEN 1 ELSE 0 END) AS underEvaluationSubmissions,
            SUM(CASE WHEN p.status = 'NEEDS_REVISION' THEN 1 ELSE 0 END) AS needsRevisionSubmissions,
            SUM(CASE WHEN p.status = 'ACCEPTED' THEN 1 ELSE 0 END) AS acceptedSubmissions,
            SUM(CASE WHEN p.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejectedSubmissions,
            SUM(CASE WHEN p.status = 'NOT_SUBMITTED' THEN 1 ELSE 0 END) AS notSubmittedSubmissions
        FROM Paper p
        WHERE p.congress.code = :congressCode
    """)
    AcademicMetricsProjection getAcademicMetricsByCongressCode(@Param("congressCode") String congressCode);
}