package com.tfi.gestion_congresos_backend.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.tfi.gestion_congresos_backend.enums.PaymentStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperPaymentResponseDTO {

    private Long paperPaymentId;
    private Long paperId; 
    private BigDecimal amountPaid;
    private LocalDateTime uploadDate;
    private String fileName;
    private String filePath;
    private String observations;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private String createdBy;
}