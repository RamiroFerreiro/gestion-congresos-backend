package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.UpdatePaymentStatusDTO;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.PaperPayment;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.PaymentStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
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

    ///-------------------------------------------CARGAR/ACTUALIZAR COMPROBANTE---------------------------------------------------------///

    @Override
    @Transactional
    public PaperPaymentResponseDTO uploadPayment(String code, MultipartFile file) {

        //Obtener usuario actual, sino lanza excepción.
        User currentUser = userService.getAuthenticatedUserEntity();

        //Validar que el Paper exista, sino lanza excepción.
        Paper paper = paperService.getPaperEntityByCode(code);
        Long paperId = paper.getPaperId();

        // Validar rol expositor
        if (!isRole(currentUser, RoleName.EXPOSITOR)) {
            throw new UserDisabledException("Solo los usuarios con rol EXPOSITOR pueden subir comprobantes de pago.");
        }
        
        //Verificar que el Paper esté en estado APROBADO
        if (paper.getStatus() != PaperStatus.ACCEPTED) {
            throw new ArgumentNotValidException("Solo se pueden subir comprobantes de pago para trabajos que hayan sido aprobados. Estado actual: " + paper.getStatus());
        } 

        //Validar que el usuario forme parte de este Paper, sino lanza excepción
        PaperAuthor authorRelation = getPaperAuthorByPaperIdAndUserId(paperId, currentUser.getUserId());

        //Validar que el usuario sea el 'main_author' de este Paper
        if (!authorRelation.isMainAuthor()) {
            throw new UserDisabledException("Solo el autor principal del trabajo está autorizado a subir el comprobante de pago.");
        }

        //Almacenar el archivo físicamente en disco
        //Traemos el comprobante, si existe borramos el archivo
        Optional<PaperPayment> existingPaymentOpt = paymentRepository.findByPaper_PaperId(paperId);

        //Si existe un comprobante y su estado ya es APROBADO, bloqueamos la subida
        if (existingPaymentOpt.isPresent() && existingPaymentOpt.get().getStatus() == PaymentStatus.APPROVED) {
            throw new ArgumentNotValidException("El comprobante de pago para este trabajo ya ha sido APROBADO. No es posible modificarlo ni re-subir otro.");
        }

        //Si existía un comprobante previo (en PENDING_APPROVAL o REJECTED) y tiene ruta de archivo, lo eliminamos
        if (existingPaymentOpt.isPresent() && existingPaymentOpt.get().getFilePath() != null) {
            fileStorageService.delete(existingPaymentOpt.get().getFilePath());
        }

        //Guardamos el nuevo archivo
        String savedPath = fileStorageService.store(file, paperId);

        //Traer el comprobante existente o instanciar uno nuevo si re-sube el comprobante
        PaperPayment payment = existingPaymentOpt.orElseGet(() -> PaperPayment.builder().paper(paper).build());

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

    ///-------------------------------------------CONSULTAR ARCHIVO---------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public Resource getPaymentFile(String paperCode) {

        //Obtener usuario autenticado
        User currentUser = userService.getAuthenticatedUserEntity();

        //Obtener Paper por código, sino existe lanza excepción
        Paper paper = paperService.getPaperEntityByCode(paperCode);

        //Validar acceso consolidado (ADMINISTRATOR o EXPOSITOR perteneciente al trabajo)
        validateReadAccess(currentUser, paper);

        //Buscar el pago o lanzar 404
        PaperPayment payment = getPaymentEntityByPaperId(paper.getPaperId(), paperCode);

        //Cargar recurso físico
        return fileStorageService.loadAsResource(payment.getFilePath());
    }

    ///-------------------------------------------CONSULTAR INFORMACION DEL COMPROBANTE---------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public PaperPaymentResponseDTO getPaymentDetails(String paperCode) {

        //Obtener usuario autenticado
        User currentUser = userService.getAuthenticatedUserEntity();

        //Obtener Paper o lanzar 404 si no existe
        Paper paper = paperService.getPaperEntityByCode(paperCode);
        
        // Validar acceso consolidado (ADMINISTRATOR o EXPOSITOR perteneciente al trabajo)
        validateReadAccess(currentUser, paper);

        //Buscar el pago o lanzar 404
        PaperPayment payment = getPaymentEntityByPaperId(paper.getPaperId(), paperCode);

        //Mapear y retornar DTO
        return paymentMapper.toResponseDTO(payment);
    }

    ///-------------------------------------------EVALUAR COMPROBANTE---------------------------------------------------------///

    @Override
    @Transactional
    public PaperPaymentResponseDTO updatePaymentStatus(String paperCode, UpdatePaymentStatusDTO updateDTO) {

        //Obtener usuario autenticado
        User currentUser = userService.getAuthenticatedUserEntity();

        //Validar que sea estrictamente ADMINISTRATOR
        if (!isRole(currentUser, RoleName.ADMINISTRATOR)) {
            throw new UserDisabledException("Solo los usuarios con rol ADMINISTRATOR pueden evaluar o cambiar el estado del comprobante.");
        }

        //Obtener el Paper, sino existe lanza excepción
        Paper paper = paperService.getPaperEntityByCode(paperCode);

        //Buscar el registro del pago
        PaperPayment payment = getPaymentEntityByPaperId(paper.getPaperId(), paperCode);

        //Regla de negocio: Si se rechaza, la observación es obligatoria
        if (updateDTO.getStatus() == PaymentStatus.REJECTED && 
        (updateDTO.getObservations() == null || updateDTO.getObservations().trim().isEmpty())) {
            throw new ArgumentNotValidException("Debe ingresar una observación explicando el motivo del rechazo del comprobante.");
        }

        //Seteamos status y observations
        payment.setStatus(updateDTO.getStatus());
        payment.setObservations(updateDTO.getObservations());

        //Guardar cambios 
        PaperPayment updatedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponseDTO(updatedPayment);
    }

    ///PRIVADOS
    private PaperAuthor getPaperAuthorByPaperIdAndUserId(Long paperId, Long userId) {
        return paperAuthorRepository.findByPaper_PaperIdAndAuthor_UserId(paperId, userId)
                .orElseThrow(() -> new UserDisabledException("El usuario no forma parte de los autores de este trabajo."));
    }

    /**
     * Verifica si el usuario posee un rol específico.
     */
    private boolean isRole(User user, RoleName roleName) {
        return user.getRole() != null && user.getRole().getName() == roleName;
    }


    /**
     * Valida si el usuario tiene acceso de lectura al comprobante.
     * Permitido para ADMINISTRATOR globalmente, o EXPOSITOR si pertenece al grupo de autores.
     */
    private void validateReadAccess(User user, Paper paper) {

        boolean isAdmin = isRole(user, RoleName.ADMINISTRATOR);
        boolean isExpositor = isRole(user, RoleName.EXPOSITOR);

        if (!isAdmin && !isExpositor) {
            throw new UserDisabledException("Su rol de usuario no tiene acceso a la consulta de comprobantes.");
        }

        if (isExpositor) {
            getPaperAuthorByPaperIdAndUserId(paper.getPaperId(), user.getUserId());
        }
    }

    /**
     * Busca la entidad de pago asociada al trabajo o lanza 404 si no existe.
     */
    private PaperPayment getPaymentEntityByPaperId(Long paperId, String paperCode) {
        return paymentRepository.findByPaper_PaperId(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún comprobante registrado para el trabajo: " + paperCode));
    }
}