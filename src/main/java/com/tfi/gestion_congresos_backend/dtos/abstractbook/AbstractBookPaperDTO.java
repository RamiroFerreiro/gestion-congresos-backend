package com.tfi.gestion_congresos_backend.dtos.abstractbook;

import java.util.List;

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
public class AbstractBookPaperDTO {

    private String code;
    private String title;
    private String thematicArea;
    private String summary;
    private List<String> keywords;
    private List<AbstractBookAuthorDTO> authors;
}
