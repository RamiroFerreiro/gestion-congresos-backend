package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.AuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.PaperResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;

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
    
    // Mapeo desde PaperAuthor -> AuthorResponseDTO:
    @Mapping(target = "authorId", source = "author.userId")
    @Mapping(target = "fullName", expression = "java(paperAuthor.getAuthor() != null ? paperAuthor.getAuthor().getFirstName() + \" \" + paperAuthor.getAuthor().getLastName() : null)")
    @Mapping(target = "email", source = "author.email")
    @Mapping(target = "dni", source = "author.dni")
    @Mapping(target = "institution", source = "author.institution")
    @Mapping(target = "country", source = "author.country")
    @Mapping(target = "authorOrder", source = "authorOrder")
    AuthorResponseDTO toAuthorResponseDTO(PaperAuthor paperAuthor);
}