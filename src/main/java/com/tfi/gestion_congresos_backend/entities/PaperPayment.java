package com.tfi.gestion_congresos_backend.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tfi.gestion_congresos_backend.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "paper_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperPayment extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paper_payment_id")
    private Long paperPaymentId;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status; // Enum: PENDING_APPROVAL, APPROVED, REJECTED

    @Column(name = "observations", length = 255)
    private String observations;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "amount_paid", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountPaid;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false, unique = true)
    private Paper paper;
}