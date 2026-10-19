package com.tfi.gestion_congresos_backend.dtos;

import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;

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

	private String userCode;
    private String fullName;
    private String email;
    private String dni;
    private String institution;
    private String country;
    private long authorOrder;
    private boolean isMainAuthor;
    private PaperAuthorStatus status;
}
