package com.tfi.gestion_congresos_backend.services.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperPayment;
import com.tfi.gestion_congresos_backend.enums.PaymentStatus;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.PaperPaymentMapper;
import com.tfi.gestion_congresos_backend.repository.PaperPaymentRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.services.FileStorageService;
import com.tfi.gestion_congresos_backend.services.PaperPaymentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaperPaymentServiceImpl implements PaperPaymentService {

    private final PaperPaymentRepository paymentRepository;
    private final PaperRepository paperRepository;
    private final FileStorageService fileStorageService;
    private final PaperPaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaperPaymentResponseDTO uploadPayment(Long paperId, MultipartFile file) {

        //Validar que el Paper exista
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo (Paper) no encontrado con id: " + paperId));

        //Almacenar el archivo físicamente en disco
        String savedPath = fileStorageService.store(file, paperId);

        //Obtener pago existente o instanciar uno nuevo si re-sube el comprobante
        PaperPayment payment = paymentRepository.findByPaper_PaperId(paperId)
                .orElse(PaperPayment.builder().paper(paper).build());

        //Asignar datos del comprobante
        payment.setFileName(file.getOriginalFilename());
        payment.setFilePath(savedPath);
        payment.setUploadDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING_APPROVAL); 
        
        // Asignación temporal de prueba (se resolverá automáticamente desde CongressPaperType más adelante)
        payment.setAmountPaid(BigDecimal.ZERO); 
        payment.setObservations(null);

        //Guardar en base de datos
        PaperPayment savedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponseDTO(savedPayment);
    }
}