package com.tfi.gestion_congresos_backend.services;

import java.util.List;

import com.tfi.gestion_congresos_backend.dtos.abstractbook.AbstractBookPaperDTO;

public interface AbstractBookService {

    List<AbstractBookPaperDTO> getEligiblePapers(String congressCode);

    byte[] generateAbstractBookWord(String congressCode);
}
