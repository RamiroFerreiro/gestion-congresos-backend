package com.tfi.gestion_congresos_backend.dtos.abstractbook;

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
public class AbstractBookAuthorDTO {

    private Long authorOrder;
    private String firstName;
    private String lastName;
    private String institution;
    private String email;
}
