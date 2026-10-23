package com.tfi.gestion_congresos_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;

@Repository
public interface PaperAuthorRepository extends JpaRepository<PaperAuthor, Long> {

    Optional<PaperAuthor> findByPaper_PaperIdAndAuthor_UserId(Long paperId, Long userId);
    boolean existsByPaper_PaperIdAndAuthor_UserId(Long paperId, Long userId);
    
    Optional<PaperAuthor> findByPaper_CodeAndAuthor_Code(String paperCode, String userCode);

    boolean existsByPaper_CodeAndAuthor_UserId(String paperCode, Long userId);

    @Query("SELECT pa FROM PaperAuthor pa JOIN FETCH pa.author WHERE pa.paper.code = :paperCode ORDER BY pa.authorOrder ASC")
    List<PaperAuthor> findByPaper_CodeOrderByAuthorOrderAsc(@Param("paperCode") String paperCode);

    @Query("SELECT pa FROM PaperAuthor pa JOIN FETCH pa.author JOIN FETCH pa.paper p WHERE p.congress.code = :congressCode")
    List<PaperAuthor> findByPaper_Congress_Code(@Param("congressCode") String congressCode);

    long countByPaper_CodeAndStatus(String paperCode, PaperAuthorStatus status);

    Optional<PaperAuthor> findByPaper_CodeAndIsMainAuthorTrue(String paperCode);
}

