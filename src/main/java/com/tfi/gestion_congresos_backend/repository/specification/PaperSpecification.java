package com.tfi.gestion_congresos_backend.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.tfi.gestion_congresos_backend.dtos.paper.PaperFilterDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;

import jakarta.persistence.criteria.Predicate;

public class PaperSpecification {

    public static Specification<Paper> filterPapers(String congressCode, PaperFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtro obligatorio por Código de Congreso
            predicates.add(cb.equal(root.get("congress").get("code"), congressCode));

            if (filter != null) {
                // 2. Filtro por Estado
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                // 3. Filtro por Sin Evaluador Asignado
                if (Boolean.TRUE.equals(filter.getUnassigned())) {
                    predicates.add(cb.isNull(root.get("userReviewer")));
                } else if (Boolean.FALSE.equals(filter.getUnassigned())) {
                    predicates.add(cb.isNotNull(root.get("userReviewer")));
                }

                // 4. Filtro por Estado de Pago
                if (filter.getPay() != null) {
                    predicates.add(cb.equal(root.get("pay"), filter.getPay()));
                }

                // 5. Filtro por Área Temática
                if (StringUtils.hasText(filter.getThematicArea())) {
                    predicates.add(cb.equal(root.get("thematicArea"), filter.getThematicArea()));
                }

                // 6. Búsqueda por texto (Título o Código)
                if (StringUtils.hasText(filter.getSearch())) {
                    String searchTerm = "%" + filter.getSearch().toLowerCase() + "%";
                    Predicate titleMatch = cb.like(cb.lower(root.get("title")), searchTerm);
                    Predicate codeMatch = cb.like(cb.lower(root.get("code")), searchTerm);
                    predicates.add(cb.or(titleMatch, codeMatch));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
