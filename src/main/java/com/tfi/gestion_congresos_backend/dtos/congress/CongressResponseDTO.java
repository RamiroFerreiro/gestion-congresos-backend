package com.tfi.gestion_congresos_backend.dtos.congress;

import java.time.LocalDateTime;
import java.util.Set;

import com.tfi.gestion_congresos_backend.dtos.user.UserResponseDTO;
import com.tfi.gestion_congresos_backend.enums.EvaluationReleaseMode;

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
	private boolean isFree;
	private boolean isSingleBlind;
	private String code;
	private EvaluationReleaseMode evaluationReleaseMode;
	private LocalDateTime scheduledEvaluationReleaseDate;
	private LocalDateTime lastEvaluationReleaseDate;
	private String contactMail;
}
