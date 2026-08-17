package com.tfi.gestion_congresos_backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponseDTO {

	private Long authorId;
	private String fullName;
	private String email;
	private Long dni;
	private String institution;
	private String country;
	private int authorOrder;
}
