package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.CongressBankDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CongressBankDetailRepository extends JpaRepository<CongressBankDetail, Long>{
    
    boolean existsByCongress_CongressId(Long congressId);

    Optional<CongressBankDetail> findByCongress_CongressId(Long congressId);
}
