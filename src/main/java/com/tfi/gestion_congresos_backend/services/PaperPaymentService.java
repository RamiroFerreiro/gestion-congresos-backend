package com.tfi.gestion_congresos_backend.services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;

public interface PaperPaymentService {

    PaperPaymentResponseDTO uploadPayment(String code, MultipartFile file);

    Resource getPaymentFile(String paperCode);
    
}
