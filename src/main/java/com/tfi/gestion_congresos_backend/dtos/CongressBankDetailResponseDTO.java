package com.tfi.gestion_congresos_backend.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressBankDetailResponseDTO {

    private Long congressBankDetailsId;
    private String bankName;
    private String accountHolder;
    private String cbuCvu;
    private String cuitCuil;
    private String alias;
    private Long congressId;
    
    // Campos de auditoría
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
