package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.paper.*;
import com.tfi.gestion_congresos_backend.entities.Paper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

// Usamos PaperAuthorMapper para que MapStruct sepa cómo mapear la lista de autores
@Mapper(componentModel = "spring", uses = {PaperAuthorMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
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
    @Mapping(target = "authors", expression = "java(paper.getCongress() != null && paper.getCongress().isSingleBlind() ? null : paperAuthorMapper.toPaperAuthorResponseDTOList(paper.getAuthors()))")
    PaperReviewerResponseDTO toPaperReviewerResponseDTO(Paper paper);

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