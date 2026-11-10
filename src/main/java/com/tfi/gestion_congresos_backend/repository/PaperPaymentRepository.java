package com.tfi.gestion_congresos_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tfi.gestion_congresos_backend.entities.PaperPayment;
import com.tfi.gestion_congresos_backend.repository.projection.FinancialMetricsProjection;

@Repository
public interface PaperPaymentRepository extends JpaRepository<PaperPayment, Long> {

    Optional<PaperPayment> findByPaper_PaperId(Long paperId);

    boolean existsByPaper_PaperId(Long paperId);

    @Query("""
        SELECT 
            COUNT(pp) AS totalPayments,
            SUM(CASE WHEN pp.status = 'PENDING_APPROVAL' THEN 1 ELSE 0 END) AS pendingApprovalPayments,
            SUM(CASE WHEN pp.status = 'APPROVED' THEN 1 ELSE 0 END) AS approvedPayments,
            SUM(CASE WHEN pp.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejectedPayments
        FROM PaperPayment pp
        WHERE pp.paper.congress.code = :congressCode
    """)
    FinancialMetricsProjection getFinancialMetricsByCongressCode(@Param("congressCode") String congressCode);
}