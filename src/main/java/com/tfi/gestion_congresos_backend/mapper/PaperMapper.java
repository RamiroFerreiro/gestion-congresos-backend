package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaperMapper {

    @Mapping(target = "congressId", source = "congress.congressId")
    @Mapping(target = "congressName", source = "congress.name")
    @Mapping(target = "reviewerId", source = "userReviewer.userId")
    @Mapping(target = "reviewerFullName", expression = "java(paper.getUserReviewer() != null ? paper.getUserReviewer().getFirstName() + \" \" + paper.getUserReviewer().getLastName() : null)")
    PaperResponseDTO toPaperResponseDTO(Paper paper);

    List<PaperResponseDTO> toPaperResponseDTOList(List<Paper> papers);
}