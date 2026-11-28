package com.tfi.gestion_congresos_backend.dtos.paper;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ReplaceKeywordsRequestDTO {
    @NotEmpty(message = "Debe ingresar al menos una palabra clave")
    private List<String> keywords;
}
