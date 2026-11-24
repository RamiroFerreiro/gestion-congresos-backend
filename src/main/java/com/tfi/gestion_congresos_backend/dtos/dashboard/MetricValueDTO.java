package com.tfi.gestion_congresos_backend.dtos.dashboard;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricValueDTO {
    
    private long count;          // Valor absoluto
    private double percentage;   // Porcentaje respecto al total
}