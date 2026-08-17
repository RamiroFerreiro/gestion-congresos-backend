package com.tfi.gestion_congresos_backend.dtos;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongressRequestDTO {
	
	@NotBlank(message = "El nombre del congreso no puede estar vacío")
	private String name;
	
	@NotBlank(message = "El lugar del congreso no puede estar vacío")
	private String place;
	
	@NotNull(message = "La fecha de inicio de inscripción es obligatoria")
    @Future(message = "La fecha de inicio de inscripción debe ser una fecha futura")
	private LocalDateTime registrationStartDate;
	
	@NotNull(message = "La fecha de fin de inscripción es obligatoria")
    @Future(message = "La fecha de fin de inscripción debe ser una fecha futura")
	private LocalDateTime registrationEndDate;
	
	@NotNull(message = "La fecha de inicio de presentación es obligatoria")
    @Future(message = "La fecha de inicio de presentación debe ser una fecha futura")
	private LocalDateTime presentationStartDate;
	
	@NotNull(message = "La fecha de fin de presentación es obligatoria")
    @Future(message = "La fecha de fin de presentación debe ser una fecha futura")
	private LocalDateTime presentationEndDate;
	
	@NotNull(message = "La fecha de inicio del congreso es obligatoria")
    @Future(message = "La fecha de inicio del congreso debe ser una fecha futura")
	private LocalDateTime startDate;
	
	@NotNull(message = "La fecha de fin del congreso es obligatoria")
    @Future(message = "La fecha de fin del congreso debe ser una fecha futura")
	private LocalDateTime endDate;
	
	@Min(value = 1, message = "Si se especifica un límite, el número máximo de autores debe ser al menos 1")
	private Integer maxNumberOfAuthors;
	
	private boolean keywordRepetition;
	
	@Min(value = 1, message = "Si se especifica un límite, el número mínimo de palabras clave debe ser al menos 1")
	private Integer minKeywords;
	
	@Min(value = 1, message = "Si se especifica un límite, el número máximo de palabras clave debe ser al menos 1")
	private Integer maxKeywords;
	
	@NotEmpty(message = "Debe especificar al menos un área temática")
	private Set<String> thematicAreas;
}
