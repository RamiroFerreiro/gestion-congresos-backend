package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    // Te trae las evaluaciones de un paper ordenadas por fecha descendente
    List<Evaluation> findByPaper_PaperIdOrderByEvaluationDateDesc(Long paperId);
}