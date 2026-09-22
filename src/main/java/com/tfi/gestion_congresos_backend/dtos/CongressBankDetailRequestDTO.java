package com.tfi.gestion_congresos_backend.dtos;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressBankDetailRequestDTO {

    @NotBlank(message = "El nombre del banco es obligatorio")
    @Size(max = 45, message = "El nombre del banco no puede superar los 45 caracteres")
    private String bankName;

    @NotBlank(message = "El titular de la cuenta es obligatorio")
    @Size(max = 255, message = "El titular de la cuenta no puede superar los 255 caracteres")
    private String accountHolder;

    @NotBlank(message = "El CBU/CVU es obligatorio")
    @Size(min = 22, max = 22, message = "El CBU/CVU debe contener exactamente 22 dígitos")
    private String cbuCvu;

    @NotBlank(message = "El CUIT/CUIL es obligatorio")
    @Size(max = 20, message = "El CUIT/CUIL no puede superar los 20 caracteres")
    private String cuitCuil;

    @NotBlank(message = "El alias es obligatorio")
    @Size(max = 255, message = "El alias no puede superar los 255 caracteres")
    private String alias;
}