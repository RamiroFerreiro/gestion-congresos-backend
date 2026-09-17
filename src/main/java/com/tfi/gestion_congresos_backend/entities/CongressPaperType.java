package com.tfi.gestion_congresos_backend.entities;

import java.math.BigDecimal;

import com.tfi.gestion_congresos_backend.enums.PaperType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "congress_paper_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressPaperType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "congress_paper_type_id")
    private Long congressPaperTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "paper_type", nullable = false)
    private PaperType paperType; 

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "congress_id", nullable = false)
    private Congress congress;
}
