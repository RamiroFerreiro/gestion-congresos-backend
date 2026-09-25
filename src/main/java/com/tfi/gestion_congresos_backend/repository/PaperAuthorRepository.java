package com.tfi.gestion_congresos_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tfi.gestion_congresos_backend.entities.PaperAuthor;

@Repository
public interface PaperAuthorRepository extends JpaRepository<PaperAuthor, Long> {

    Optional<PaperAuthor> findByPaper_PaperIdAndAuthor_UserId(Long paperId, Long userId);
}
