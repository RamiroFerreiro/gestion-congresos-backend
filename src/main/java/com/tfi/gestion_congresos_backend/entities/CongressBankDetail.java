package com.tfi.gestion_congresos_backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "congress_bank_details")
public class CongressBankDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "congress_bank_details_id")
    private Long congressBankDetailsId;

    @Column(name = "bank_name", length = 45)
    private String bankName;

    @Column(name = "account_holder", length = 255)
    private String accountHolder;

    @Column(name = "cbu_cvu", length = 22)
    private String cbuCvu;

    @Column(name = "cuit_cuil", length = 20)
    private String cuitCuil;

    @Column(name = "alias", length = 255)
    private String alias;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "congress_id", nullable = false, unique = true)
    private Congress congress;
    
}
