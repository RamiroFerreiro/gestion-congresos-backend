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
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
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
    private final SecurityEvaluator securityEvaluator;

    ///-------------------------------------------CARGAR/ACTUALIZAR COMPROBANTE---------------------------------------------------------///

    ///EL ROL NO ESTÁ MÁS EN LA ENTIDAD USER
    @Override
    @Transactional
    public PaperPaymentResponseDTO uploadPayment(String paperCode, MultipartFile file) {

        //0. Validar que el archivo no esté vacío
        if (file == null || file.isEmpty()) throw new ArgumentNotValidException("El archivo es obligatorio");
        
        //1. Obtener el usuario autenticado
        User authenticatedUser = userService.getAuthenticatedUserEntity();

        //2. Validar que el Paper exista (Lanza ResourceNotFoundException si no existe)
        Paper paper = paperService.getPaperEntityByCode(paperCode);
        Long paperId = paper.getPaperId();
        String congressCode = paper.getCongress().getCode(); 
        
        //3. Validar rol expositor de ese congreso
        if (!securityEvaluator.isExpositorOfCongress(authenticatedUser, congressCode)) {
            throw new UserDisabledException("Solo los usuarios con rol EXPOSITOR pueden subir comprobantes de pago.");
        }
        
        //4. Validar que el usuario forme parte de este Paper, sino lanza excepción
        PaperAuthor authorRelation = getPaperAuthorByPaperIdAndUserId(paperId, authenticatedUser.getUserId());

        //5. Validar que el usuario sea el 'main_author' de este Paper
        if (!authorRelation.isMainAuthor()) {
            throw new UserDisabledException("Solo el autor principal del trabajo está autorizado a subir el comprobante de pago.");
        }
        
        //6. Verificar que el Paper esté en estado APROBADO
        if (paper.getStatus() != PaperStatus.ACCEPTED) {
            throw new ArgumentNotValidException("Solo se pueden subir comprobantes de pago para trabajos que hayan sido aprobados. Estado actual: " + paper.getStatus());
        } 

        //7. Consultar si ya existe un comprobante previo y validar su estado
        //Traemos el comprobante
        Optional<PaperPayment> existingPaymentOpt = paymentRepository.findByPaper_PaperId(paperId);

        //8. Si existe un comprobante y su estado ya es APROBADO, bloqueamos la subida
        if (existingPaymentOpt.isPresent() && existingPaymentOpt.get().getStatus() == PaymentStatus.APPROVED) {
            throw new ArgumentNotValidException("El comprobante de pago para este trabajo ya ha sido APROBADO. No es posible modificarlo ni re-subir otro.");
        }

        // ----------------------------------------------------------------------------------
        // HASTA AQUÍ TODAS LAS VALIDACIONES PASARON -> RECIÉN OPERAMOS EN DISCO
        // ----------------------------------------------------------------------------------

        //9. Si existía un comprobante previo (en PENDING_APPROVAL o REJECTED) y tiene ruta de archivo, lo eliminamos
        if (existingPaymentOpt.isPresent() && existingPaymentOpt.get().getFilePath() != null) {
            fileStorageService.delete(existingPaymentOpt.get().getFilePath());
        }

        //10. Guardamos el nuevo archivo
        String savedPath = fileStorageService.store(file, paperId);

        //12. Traer el comprobante existente o instanciar uno nuevo si re-sube el comprobante
        PaperPayment payment = existingPaymentOpt.orElseGet(() -> PaperPayment.builder().paper(paper).build());

        //12. Asignar datos del comprobante
        payment.setFileName(file.getOriginalFilename());
        payment.setFilePath(savedPath);
        payment.setUploadDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING_APPROVAL); 
        payment.setAmountPaid(paper.getCongressPaperType().getAmount()); 
        payment.setObservations(null);

        //13. Persistir en la base de datos y retornar DTO
        PaperPayment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponseDTO(savedPayment);
    }

    ///-------------------------------------------CONSULTAR ARCHIVO---------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public Resource getPaymentFile(String paperCode) {

        //1. Obtener usuario autenticado
        User authenticatedUser = userService.getAuthenticatedUserEntity();

        //2. Obtener Paper por código, sino existe lanza excepción
        Paper paper = paperService.getPaperEntityByCode(paperCode);

        //3. Validar acceso consolidado (ADMINISTRATOR o EXPOSITOR perteneciente al trabajo)
        validateReadAccess(authenticatedUser, paper);

        //4. Buscar el pago o lanzar 404
        PaperPayment payment = getPaymentEntityByPaperId(paper.getPaperId(), paperCode);

        //5. Cargar recurso físico
        return fileStorageService.loadAsResource(payment.getFilePath());
    }

    ///-------------------------------------------CONSULTAR INFORMACION DEL COMPROBANTE---------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public PaperPaymentResponseDTO getPaymentDetails(String paperCode) {

        //1. Obtener usuario autenticado
        User authenticatedUser = userService.getAuthenticatedUserEntity();

        //2. Obtener Paper o lanzar 404 si no existe
        Paper paper = paperService.getPaperEntityByCode(paperCode);
        
        //3. Validar acceso consolidado (ADMINISTRATOR o EXPOSITOR perteneciente al trabajo)
        validateReadAccess(authenticatedUser, paper);

        //4. Buscar el pago o lanzar 404
        PaperPayment payment = getPaymentEntityByPaperId(paper.getPaperId(), paperCode);

        //5. Mapear y retornar DTO
        return paymentMapper.toResponseDTO(payment);
    }

    ///-------------------------------------------EVALUAR COMPROBANTE---------------------------------------------------------///
    @Override
    @Transactional
    public PaperPaymentResponseDTO updatePaymentStatus(String paperCode, UpdatePaymentStatusDTO updateDTO) {

        //1. Obtener usuario autenticado
        User authenticatedUser = userService.getAuthenticatedUserEntity();

        //2. Validar que el Paper exista (Lanza ResourceNotFoundException si no existe)
        Paper paper = paperService.getPaperEntityByCode(paperCode);
        String congressCode = paper.getCongress().getCode(); 
        
        //3. Validar que sea estrictamente ADMINISTRATOR del congreso en cuestión
        if(!securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)){
            throw new UserDisabledException("Solo los usuarios con rol ADMINISTRATOR pueden evaluar comprobantes de pago.");
        }

        //4. Buscar el registro del pago
        PaperPayment payment = getPaymentEntityByPaperId(paper.getPaperId(), paperCode);

        //5. REGLA DE NEGOCIO: Si el pago ya fue APROBADO, no se puede cambiar su estado
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            throw new ArgumentNotValidException("El comprobante de pago ya fue APROBADO previamente y no se puede modificar su estado.");
        }

        //6. Regla de negocio: Si se rechaza, la observación es obligatoria
        if (updateDTO.getStatus() == PaymentStatus.REJECTED && 
        (updateDTO.getObservations() == null || updateDTO.getObservations().trim().isEmpty())) {
            throw new ArgumentNotValidException("Debe ingresar una observación explicando el motivo del rechazo del comprobante.");
        }

        //7. Setear status y observations
        payment.setStatus(updateDTO.getStatus());
        payment.setObservations(updateDTO.getObservations());

        // 8. Sincronizar el flag de pago en la entidad Paper si fue aprobado
        if (updateDTO.getStatus() == PaymentStatus.APPROVED) {
            paper.setPay(true);
        }

        //9. Guardar cambios y retornar DTO
        PaperPayment updatedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponseDTO(updatedPayment);
    }

    ///PRIVADOS
    private PaperAuthor getPaperAuthorByPaperIdAndUserId(Long paperId, Long userId) {
        return paperAuthorRepository.findByPaper_PaperIdAndAuthor_UserId(paperId, userId)
                .orElseThrow(() -> new UserDisabledException("El usuario no forma parte de los autores de este trabajo."));
    }


    ///EL ROL NO ESTÁ MÁS EN LA ENTIDAD USER
    /**
     * Verifica si el usuario posee un rol específico.
     
    private boolean isRole(User user, RoleName roleName) {
        return user.getRole() != null && user.getRole().getName() == roleName;
    }
    */


    ///EL ROL NO ESTÁ MÁS EN LA ENTIDAD USER
    /**
     * Valida si el usuario tiene acceso de lectura al comprobante.
     * Permitido para ADMINISTRATOR globalmente, o EXPOSITOR si pertenece al grupo de autores.
     */
    private void validateReadAccess(User user, Paper paper) {

        String congressCode = paper.getCongress().getCode();

        // 1. Si es Administrador del congreso (o Súper Admin), tiene acceso total
        if (securityEvaluator.isAdminOfCongress(user, congressCode)) {
            return;
        }

        // 2. Si no es Admin, debe ser Expositor en el congreso especificado
        if (!securityEvaluator.isExpositorOfCongress(user, congressCode)) {
            throw new UserDisabledException("Solo los usuarios con rol EXPOSITOR pueden ver comprobantes de pago.");
        }

        // 3. Además de ser Expositor, debe estar registrado como participante/autor de este Paper específico
        if (!securityEvaluator.isParticipantOfPaper(paper.getPaperId(), user.getUserId())) {
            throw new UserDisabledException("Solo los autores pertenecientes a este trabajo pueden ver el comprobante de pago.");
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