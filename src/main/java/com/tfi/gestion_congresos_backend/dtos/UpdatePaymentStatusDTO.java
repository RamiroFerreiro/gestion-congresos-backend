package com.tfi.gestion_congresos_backend.dtos;
import com.tfi.gestion_congresos_backend.enums.PaymentStatus;

import jakarta.validation.constraints.NotNull;
import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePaymentStatusDTO {

    @NotNull(message = "El estado del pago es obligatorio.")
    private PaymentStatus status;

    private String observations; // Obligatorio solo en caso de REJECTED
}