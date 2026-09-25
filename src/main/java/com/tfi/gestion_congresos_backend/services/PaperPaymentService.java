package com.tfi.gestion_congresos_backend.services;

import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;

public interface PaperPaymentService {

    PaperPaymentResponseDTO uploadPayment(String code, MultipartFile file);
    
}
