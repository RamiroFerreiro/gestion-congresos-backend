
package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.Paper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {


}