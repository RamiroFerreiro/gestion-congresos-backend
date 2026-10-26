package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.CongressPaperType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CongressPaperTypeRepository extends JpaRepository<CongressPaperType, Long> {
}