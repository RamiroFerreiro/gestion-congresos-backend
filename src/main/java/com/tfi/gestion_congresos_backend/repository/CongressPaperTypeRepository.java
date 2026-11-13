package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.CongressPaperType;
import com.tfi.gestion_congresos_backend.enums.PaperType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CongressPaperTypeRepository extends JpaRepository<CongressPaperType, Long> {

    Optional<CongressPaperType> findByCongress_CodeAndPaperType(
            String congressCode,
            PaperType paperType
    );
}
