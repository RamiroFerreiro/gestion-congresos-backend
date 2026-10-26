package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.paper.PaperAuthorResponseDTO;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class PaperAuthorMapper {

    @Autowired
    protected CongressParticipantRepository participantRepository;

    @Mapping(target = "userCode", source = "author.code")
    @Mapping(target = "paperCode", source = "paper.code")
    @Mapping(target = "fullName", expression = "java(paperAuthor.getAuthor() != null ? paperAuthor.getAuthor().getFirstName() + \" \" + paperAuthor.getAuthor().getLastName() : null)")
    @Mapping(target = "email", source = "author.email")
    @Mapping(target = "dni", source = "author.dni")
    @Mapping(target = "country", source = "author.country")
    @Mapping(target = "authorOrder", source = "authorOrder")
    @Mapping(target = "isMainAuthor", source = "mainAuthor")
    @Mapping(target = "isPresentator", source = "presentator")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "institution", expression = "java(getInstitution(paperAuthor))") // Llama al método custom
    public abstract PaperAuthorResponseDTO toPaperAuthorResponseDTO(PaperAuthor paperAuthor);

    public abstract List<PaperAuthorResponseDTO> toPaperAuthorResponseDTOList(List<PaperAuthor> paperAuthors);

    // Método protegido que MapStruct utilizará para resolver la institución
    protected String getInstitution(PaperAuthor paperAuthor) {
        if (paperAuthor.getAuthor() == null || paperAuthor.getPaper() == null || paperAuthor.getPaper().getCongress() == null) {
            return null;
        }
        
        return participantRepository.findUserParticipantOfCongress(
                paperAuthor.getAuthor().getCode(), 
                paperAuthor.getPaper().getCongress().getCode()
        )
        .map(CongressParticipant::getInstitution)
        .orElse("Desconocida");
    }
}