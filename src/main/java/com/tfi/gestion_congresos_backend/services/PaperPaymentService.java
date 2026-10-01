package com.tfi.gestion_congresos_backend.services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.UpdatePaymentStatusDTO;

public interface PaperPaymentService {

    PaperPaymentResponseDTO uploadPayment(String code, MultipartFile file);

    Resource getPaymentFile(String paperCode);
    
    PaperPaymentResponseDTO getPaymentDetails(String paperCode);

    PaperPaymentResponseDTO updatePaymentStatus(String paperCode, UpdatePaymentStatusDTO updateDTO);
}
