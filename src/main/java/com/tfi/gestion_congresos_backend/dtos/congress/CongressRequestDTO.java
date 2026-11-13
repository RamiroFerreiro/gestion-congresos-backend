package com.tfi.gestion_congresos_backend.dtos.congress;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import com.tfi.gestion_congresos_backend.enums.EvaluationReleaseMode;

import jakarta.validation.constraints.Email;
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
	private LocalDateTime registrationStartDate;
	
	@NotNull(message = "La fecha de fin de inscripción es obligatoria")
	private LocalDateTime registrationEndDate;
	
	@NotNull(message = "La fecha de inicio de presentación es obligatoria")
	private LocalDateTime presentationStartDate;
	
	@NotNull(message = "La fecha de fin de presentación es obligatoria")
	private LocalDateTime presentationEndDate;
	
	@NotNull(message = "La fecha de inicio del congreso es obligatoria")
	private LocalDateTime startDate;
	
	@NotNull(message = "La fecha de fin del congreso es obligatoria")
	private LocalDateTime endDate;
	
	@Min(value = 1, message = "Si se especifica un límite, el número máximo de autores debe ser al menos 1")
	private Integer maxNumberOfAuthors;
	
	private boolean keywordRepetition;
	
	@Min(value = 1, message = "Si se especifica un límite, el número mínimo de palabras clave debe ser al menos 1")
	private Integer minKeywords;
	
	@Min(value = 1, message = "Si se especifica un límite, el número máximo de palabras clave debe ser al menos 1")
	private Integer maxKeywords;
	
	@NotEmpty(message = "Debe especificar al menos un área temática")
	private Set<@NotBlank(message = "Las áreas temáticas no pueden estar vacías") String> thematicAreas;
	
	private boolean isFree;

	// Configuración del único tipo de trabajo habilitado en el MVP (ABSTRACT).
	// No forma parte de la entidad Congress: se usa solamente al crear el congreso.
	private BigDecimal abstractAmount;
	private String abstractCurrency;
	
	private boolean isSingleBlind;
	
	@NotNull(message = "El modo de publicación de evalauciones es obligatorio")
	private EvaluationReleaseMode evaluationReleaseMode;
	
	private LocalDateTime plannedEvaluationReleaseDate;
	
	@NotBlank(message = "El email de contacto del congreso no puede estar vacío")
	@Email(message = "El email de contacto no tiene un formato válido")
	private String contactMail;
}
