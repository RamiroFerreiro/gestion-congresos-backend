package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.PaperPayment;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.PaymentStatus;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.mapper.PaperPaymentMapper;
import com.tfi.gestion_congresos_backend.repository.PaperAuthorRepository;
import com.tfi.gestion_congresos_backend.repository.PaperPaymentRepository;
import com.tfi.gestion_congresos_backend.services.FileStorageService;
import com.tfi.gestion_congresos_backend.services.PaperPaymentService;
import com.tfi.gestion_congresos_backend.services.PaperService;
import com.tfi.gestion_congresos_backend.services.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaperPaymentServiceImpl implements PaperPaymentService {

    private final PaperPaymentRepository paymentRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final FileStorageService fileStorageService;
    private final PaperPaymentMapper paymentMapper;
    private final UserService userService;
    private final PaperService paperService;

    @Override
    @Transactional
    public PaperPaymentResponseDTO uploadPayment(String code, MultipartFile file) {

        //Obtener usuario actual, sino lanza excepción.
        User currentUser = userService.getAuthenticatedUserEntity();

        //Validar que el Paper exista, sino lanza excepción.
        Paper paper = paperService.getPaperEntityByCode(code);
        Long paperId = paper.getPaperId();

        //Validar que el usuario posea el rol EXPOSITOR, sino lanza excepción.
        boolean isExpositor = currentUser.getRole() != null && "EXPOSITOR".equalsIgnoreCase(currentUser.getRole().getName().name());
        
        if (!isExpositor) {
            throw new UserDisabledException("Solo los usuarios con rol EXPOSITOR pueden subir comprobantes de pago.");
        }
        
        //Verificar que el Paper esté en estado APROBADO
        if (paper.getStatus() != PaperStatus.ACCEPTED) {
            throw new ArgumentNotValidException("Solo se pueden subir comprobantes de pago para trabajos que hayan sido aprobados. Estado actual: " + paper.getStatus());
        } 

        //Validar que el usuario forme parte de este Paper
        PaperAuthor authorRelation = paperAuthorRepository.findByPaper_PaperIdAndAuthor_UserId(paperId, currentUser.getUserId())
                .orElseThrow(() -> new UserDisabledException("El usuario no forma parte de los autores de este trabajo."));

        //Validar que el usuario sea el 'main_author' de este Paper
        if (!authorRelation.isMainAuthor()) {
            throw new UserDisabledException("Solo el autor principal del trabajo está autorizado a subir el comprobante de pago.");
        }

        //Almacenar el archivo físicamente en disco
        String savedPath = fileStorageService.store(file, paperId);

        //Obtener pago existente o instanciar uno nuevo si re-sube el comprobante
        PaperPayment payment = paymentRepository.findByPaper_PaperId(paperId).orElse(PaperPayment.builder().paper(paper).build());

        //Asignar datos del comprobante
        payment.setFileName(file.getOriginalFilename());
        payment.setFilePath(savedPath);
        payment.setUploadDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING_APPROVAL); 
        payment.setAmountPaid(paper.getCongressPaperType().getAmount()); 
        payment.setObservations(null);

        //Guardar en base de datos
        PaperPayment savedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponseDTO(savedPayment);
    }
}