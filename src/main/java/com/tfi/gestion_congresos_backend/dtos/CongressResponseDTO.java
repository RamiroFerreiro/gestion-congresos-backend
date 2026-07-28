package com.tfi.gestion_congresos_backend.dtos;

import java.time.LocalDateTime;
import java.util.Set;

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
public class CongressResponseDTO {

	private Long congressId;
	private String name;
	private LocalDateTime registrationStartDate;
	private LocalDateTime registrationEndDate;
	private LocalDateTime presentationStartDate;
	private LocalDateTime presentationEndDate;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private Integer maxNumberOfAuthors;
	private boolean keywordRepetition;
	private Integer minKeywords;
	private Integer maxKeywords;
	private Set<String> thematicAreas;
	private boolean enabled;
	private Set<UserResponseDTO> participants;
}
