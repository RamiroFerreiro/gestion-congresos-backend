package com.tfi.gestion_congresos_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tfi.gestion_congresos_backend.entities.PaperPayment;

@Repository
public interface PaperPaymentRepository extends JpaRepository<PaperPayment, Long> {

    Optional<PaperPayment> findByPaper_PaperId(Long paperId);

    boolean existsByPaper_PaperId(Long paperId);
}