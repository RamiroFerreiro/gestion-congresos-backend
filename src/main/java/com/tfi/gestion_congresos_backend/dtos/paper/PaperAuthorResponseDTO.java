package com.tfi.gestion_congresos_backend.dtos.paper;

import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperAuthorResponseDTO {

    private String userCode;
    private String fullName;
    private String email;
    private String dni;
    private String institution;
    private String country;
    
    private long authorOrder;
    private boolean isMainAuthor;
    private boolean isPresentator;
    private PaperAuthorStatus status;
    
    private String paperCode; // Sabemos a que paper pertenece
}