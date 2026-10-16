package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.*;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperDetailResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperReviewerResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperSummaryResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperUpdateRequestDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaperMapper {

    // --- MAPEOS A SUMMARY ---
    @Mapping(target = "congressCode", source = "congress.code")
    @Mapping(target = "congressName", source = "congress.name")
    PaperSummaryResponseDTO toPaperSummaryResponseDTO(Paper paper);

    List<PaperSummaryResponseDTO> toPaperSummaryResponseDTOList(List<Paper> papers);

    // --- MAPEOS A DETAIL ---
    @Mapping(target = "congressCode", source = "congress.code")
    @Mapping(target = "congressName", source = "congress.name")
    @Mapping(target = "reviewerCode", source = "userReviewer.code")
    @Mapping(target = "reviewerFullName", expression = "java(paper.getUserReviewer() != null ? paper.getUserReviewer().getFirstName() + \" \" + paper.getUserReviewer().getLastName() : null)")
    @Mapping(target = "congressPaperTypeName", expression = "java(paper.getCongressPaperType() != null && paper.getCongressPaperType().getPaperType() != null ? paper.getCongressPaperType().getPaperType().name() : null)")
    PaperDetailResponseDTO toPaperDetailResponseDTO(Paper paper);

    // --- MAPEOS A REVIEWER VIEW (SINGLE BLIND HANDLER) ---
    @Mapping(target = "congressCode", source = "congress.code")
    @Mapping(target = "congressName", source = "congress.name")
    @Mapping(target = "authors", expression = "java(paper.getCongress() != null && paper.getCongress().isSingleBlind() ? null : toAuthorResponseDTOList(paper.getAuthors()))")
    PaperReviewerResponseDTO toPaperReviewerResponseDTO(Paper paper);

    // --- MAPEOS DE AUTOR ---
    @Mapping(target = "userCode", source = "author.code")
    @Mapping(target = "fullName", expression = "java(paperAuthor.getAuthor() != null ? paperAuthor.getAuthor().getFirstName() + \" \" + paperAuthor.getAuthor().getLastName() : null)")
    @Mapping(target = "email", source = "author.email")
    @Mapping(target = "dni", source = "author.dni")
    @Mapping(target = "country", source = "author.country")
    @Mapping(target = "authorOrder", source = "authorOrder")
    @Mapping(target = "isMainAuthor", source = "mainAuthor")
    @Mapping(target = "status", source = "status")
    AuthorResponseDTO toAuthorResponseDTO(PaperAuthor paperAuthor);

    List<AuthorResponseDTO> toAuthorResponseDTOList(List<PaperAuthor> paperAuthors);

    // --- MAPEO DE REQUEST A ENTIDAD ---
    @Mapping(target = "paperId", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "congress", ignore = true)
    @Mapping(target = "userReviewer", ignore = true)
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "keywords", ignore = true)
    @Mapping(target = "pay", ignore = true)
    @Mapping(target = "congressPaperType", ignore = true)
    Paper toEntity(PaperRequestDTO dto);

    @Mapping(target = "paperId", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "congress", ignore = true)
    @Mapping(target = "userReviewer", ignore = true)
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "keywords", ignore = true)
    @Mapping(target = "pay", ignore = true)
    @Mapping(target = "congressPaperType", ignore = true)
    void updateEntityFromDto(PaperUpdateRequestDTO dto, @MappingTarget Paper paper);
}