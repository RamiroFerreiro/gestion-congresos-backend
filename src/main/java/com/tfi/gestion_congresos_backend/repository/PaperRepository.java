
package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.repository.projection.AcademicMetricsProjection;
import com.tfi.gestion_congresos_backend.repository.projection.PaperSummaryProjection;
import com.tfi.gestion_congresos_backend.repository.projection.ParticipantPaperSummaryProjection;
import com.tfi.gestion_congresos_backend.repository.projection.ParticipantHomeAcademicMetricsProjection;
import com.tfi.gestion_congresos_backend.repository.projection.ParticipantHomePaymentMetricsProjection;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long>, JpaSpecificationExecutor<Paper> {

    @Query("""
        SELECT DISTINCT p
        FROM Paper p
        LEFT JOIN FETCH p.authors pa
        LEFT JOIN FETCH pa.author
        WHERE p.congress.congressId = :congressId
          AND p.status = com.tfi.gestion_congresos_backend.enums.PaperStatus.ACCEPTED
          AND (p.congress.isFree = true OR EXISTS (
              SELECT pp.paperPaymentId
              FROM PaperPayment pp
              WHERE pp.paper = p
                AND pp.status = com.tfi.gestion_congresos_backend.enums.PaymentStatus.APPROVED
          ))
        """)
    List<Paper> findEligibleForAbstractBook(@Param("congressId") Long congressId);

    /// Busca todos los papers de un congreso por su ID:
    @Query("""
        SELECT DISTINCT p 
        FROM Paper p LEFT JOIN FETCH p.authors pa 
        LEFT JOIN FETCH pa.author LEFT JOIN FETCH p.congress c 
        LEFT JOIN FETCH p.userReviewer WHERE c.congressId = :congressId 
        ORDER BY pa.authorOrder ASC
        """)
    List<Paper> findByCongressIdWithDetails(@Param("congressId") Long congressId);

    boolean existsByCode(String code);

    Optional<Paper> findByCode(String code);

    @Query(value = """
        SELECT p.code AS code,
               p.title AS title,
               p.version AS version,
               p.status AS status,
               p.submittedAt AS submittedAt,
               pa.isMainAuthor AS responsibleAuthor,
               c.isFree AS freeCongress,
               pp.status AS rawPaymentStatus,
               p.lastActivityType AS lastActivityType,
               p.lastActivityAt AS lastActivityAt
        FROM Paper p
        JOIN p.congress c
        JOIN p.authors pa
        LEFT JOIN PaperPayment pp ON pp.paper = p
        WHERE c.code = :congressCode
          AND pa.author.userId = :userId
          AND pa.status = :acceptedStatus
          AND (:status IS NULL OR p.status = :status)
        """,
        countQuery = """
        SELECT COUNT(p)
        FROM Paper p
        JOIN p.congress c
        JOIN p.authors pa
        WHERE c.code = :congressCode
          AND pa.author.userId = :userId
          AND pa.status = :acceptedStatus
          AND (:status IS NULL OR p.status = :status)
        """)
    Page<ParticipantPaperSummaryProjection> findParticipantPaperSummaries(
            @Param("congressCode") String congressCode,
            @Param("userId") Long userId,
            @Param("acceptedStatus") PaperAuthorStatus acceptedStatus,
            @Param("status") PaperStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(DISTINCT p) AS totalAssociatedPapers,
               COALESCE(SUM(CASE WHEN p.status = com.tfi.gestion_congresos_backend.enums.PaperStatus.NOT_SUBMITTED THEN 1 ELSE 0 END), 0) AS drafts,
               COALESCE(SUM(CASE WHEN p.status = com.tfi.gestion_congresos_backend.enums.PaperStatus.NEEDS_REVISION THEN 1 ELSE 0 END), 0) AS needsRevision,
               COALESCE(SUM(CASE WHEN p.status = com.tfi.gestion_congresos_backend.enums.PaperStatus.UNDER_EVALUATION THEN 1 ELSE 0 END), 0) AS underEvaluation
        FROM Paper p
        JOIN p.authors pa
        WHERE p.congress.code = :congressCode
          AND pa.author.userId = :userId
          AND pa.status = :acceptedAuthorStatus
        """)
    ParticipantHomeAcademicMetricsProjection getParticipantHomeAcademicMetrics(
            @Param("congressCode") String congressCode,
            @Param("userId") Long userId,
            @Param("acceptedAuthorStatus") PaperAuthorStatus acceptedAuthorStatus
    );

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN pp.paperPaymentId IS NULL
                              OR pp.status = com.tfi.gestion_congresos_backend.enums.PaymentStatus.REJECTED
                         THEN 1 ELSE 0 END), 0) AS paymentsWithoutValidReceipt,
               COALESCE(SUM(CASE WHEN pp.status = com.tfi.gestion_congresos_backend.enums.PaymentStatus.PENDING_APPROVAL
                         THEN 1 ELSE 0 END), 0) AS paymentsPendingApproval,
               COALESCE(SUM(CASE WHEN pp.status = com.tfi.gestion_congresos_backend.enums.PaymentStatus.APPROVED
                         THEN 1 ELSE 0 END), 0) AS paymentsVerified
        FROM Paper p
        JOIN p.authors pa
        LEFT JOIN PaperPayment pp ON pp.paper = p
        WHERE p.congress.code = :congressCode
          AND p.congress.isFree = false
          AND p.status = com.tfi.gestion_congresos_backend.enums.PaperStatus.ACCEPTED
          AND pa.author.userId = :userId
          AND pa.isMainAuthor = true
          AND pa.status = :acceptedAuthorStatus
        """)
    ParticipantHomePaymentMetricsProjection getParticipantHomePaymentMetrics(
            @Param("congressCode") String congressCode,
            @Param("userId") Long userId,
            @Param("acceptedAuthorStatus") PaperAuthorStatus acceptedAuthorStatus
    );
    
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

    @Query("""
        SELECT DISTINCT pa.author
        FROM Paper p
        JOIN p.authors pa
        WHERE p.congress.code = :congressCode
          AND p.congress.isFree = false
          AND p.status = com.tfi.gestion_congresos_backend.enums.PaperStatus.ACCEPTED
          AND p.pay = false
          AND pa.isMainAuthor = true
          AND pa.status = com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus.ACCEPTED
          AND pa.author.enabled = true
        """)
    List<com.tfi.gestion_congresos_backend.entities.User> findMainAuthorsWithPendingPaymentsByCongressCode(
            @Param("congressCode") String congressCode
    );
}
