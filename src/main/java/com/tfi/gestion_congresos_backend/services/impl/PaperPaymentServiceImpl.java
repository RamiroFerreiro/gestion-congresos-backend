package com.tfi.gestion_congresos_backend.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.dtos.PaperPaymentResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.UpdatePaymentStatusDTO;
import com.tfi.gestion_congresos_backend.entities.CongressParticipant;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.PaperPayment;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.PaymentStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.mapper.PaperPaymentMapper;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.PaperAuthorRepository;
import com.tfi.gestion_congresos_backend.repository.PaperPaymentRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.EmailService;
import com.tfi.gestion_congresos_backend.services.FileStorageService;
import com.tfi.gestion_congresos_backend.services.PaperPaymentService;
import com.tfi.gestion_congresos_backend.services.PaperService;
import com.tfi.gestion_congresos_backend.services.validation.CongressParticipantInstitutionValidator;
import com.tfi.gestion_congresos_backend.utils.DateUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperPaymentServiceImpl implements PaperPaymentService {

    private final PaperPaymentRepository paymentRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final CongressParticipantRepository congressParticipantRepository;
    private final FileStorageService fileStorageService;
    private final PaperPaymentMapper paymentMapper;
    private final PaperService paperService;
    private final AuthorizationService authorizationService;
    private final EmailService emailService;
    private final CongressParticipantInstitutionValidator institutionValidator;
    private final SecurityEvaluator securityEvaluator;

    ///-------------------------------------------CARGAR/ACTUALIZAR COMPROBANTE---------------------------------------------------------///

    @Override
    @Transactional
    public PaperPaymentResponseDTO uploadPayment(String paperCode, MultipartFile file) {

        //0. Validar que el archivo no esté vacío
        if (file == null || file.isEmpty()) throw new ArgumentNotValidException("El archivo es obligatorio");
        
        //1. Obtener el usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

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
        
        //6. Validar institución del usuario en el congreso
        validateCurrentUserInstitution(congressCode);

        // Los congresos gratuitos no requieren ni admiten comprobantes de pago.
        if (paper.getCongress().isFree()) {
            throw new ArgumentNotValidException(
                    "No se pueden cargar comprobantes de pago para un congreso gratuito."
            );
        }
        
        //7. Verificar que el Paper esté en estado APROBADO
        if (paper.getStatus() != PaperStatus.ACCEPTED) {
            throw new ArgumentNotValidException("Solo se pueden subir comprobantes de pago para trabajos que hayan sido aprobados. Estado actual: " + paper.getStatus());
        } 

        //8. Consultar si ya existe un comprobante previo y validar su estado
        //Traemos el comprobante
        Optional<PaperPayment> existingPaymentOpt = paymentRepository.findByPaper_PaperId(paperId);
        
        //9. Determinar el estado del comprobante antes de reemplazarlo
        boolean isFirstReceipt = existingPaymentOpt.isEmpty();

        boolean isReplacingPendingReceipt = existingPaymentOpt
                .map(payment -> payment.getStatus() == PaymentStatus.PENDING_APPROVAL)
                .orElse(false);
        
        boolean isReplacingRejectedReceipt = existingPaymentOpt
                .map(payment -> payment.getStatus() == PaymentStatus.REJECTED)
                .orElse(false);

        boolean shouldNotifyReceiptReceived  =
                isFirstReceipt || isReplacingPendingReceipt;

        //10. Si existe un comprobante y su estado ya es APROBADO, bloqueamos la subida
        if (existingPaymentOpt.isPresent() && existingPaymentOpt.get().getStatus() == PaymentStatus.APPROVED) {
            throw new ArgumentNotValidException("El comprobante de pago para este trabajo ya ha sido APROBADO. No es posible modificarlo ni re-subir otro.");
        }

        // ----------------------------------------------------------------------------------
        // HASTA AQUÍ TODAS LAS VALIDACIONES PASARON -> RECIÉN OPERAMOS EN DISCO
        // ----------------------------------------------------------------------------------

        //11. Si existía un comprobante previo (en PENDING_APPROVAL o REJECTED) y tiene ruta de archivo, lo eliminamos
        if (existingPaymentOpt.isPresent() && existingPaymentOpt.get().getFilePath() != null) {
            fileStorageService.delete(existingPaymentOpt.get().getFilePath());
        }

        //12. Guardamos el nuevo archivo
        String savedPath = fileStorageService.store(file, paperId);

        //13. Traer el comprobante existente o instanciar uno nuevo si re-sube el comprobante
        PaperPayment payment = existingPaymentOpt.orElseGet(() -> PaperPayment.builder().paper(paper).build());

        //14. Asignar datos del comprobante
        payment.setFileName(file.getOriginalFilename());
        payment.setFilePath(savedPath);
        payment.setUploadDate(DateUtils.now());
        payment.setStatus(PaymentStatus.PENDING_APPROVAL); 
        payment.setAmountPaid(paper.getCongressPaperType().getAmount()); 
        payment.setObservations(null);

        //15. Persistir en la base de datos
        PaperPayment savedPayment = paymentRepository.save(payment);
        
        //16. Notificar a los administradores
        if (isReplacingRejectedReceipt) {

            notifyAdministratorsAboutRejectedReceiptReplacement(savedPayment);  //Reemplazo de comprobante

        } else if (shouldNotifyReceiptReceived) {

            notifyAdministratorsAboutReceivedPayment(savedPayment); //Comprobante nuevo
        }
        
        //17. Notificar a todos los autores aceptados (PRE-M07)
        notifyAuthorsAboutReceivedPayment(savedPayment);
        
        //18. Retornar DTO
        return paymentMapper.toResponseDTO(savedPayment);
    }

    ///-------------------------------------------CONSULTAR ARCHIVO---------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public Resource getPaymentFile(String paperCode) {

        //1. Obtener usuario autenticado
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

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
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

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
        User authenticatedUser = authorizationService.getAuthenticatedUserEntity();

        //2. Validar que el Paper exista (Lanza ResourceNotFoundException si no existe)
        Paper paper = paperService.getPaperEntityByCode(paperCode);
        String congressCode = paper.getCongress().getCode();

        //3. Validar que sea administrador del congreso o súper administrador
        if (!securityEvaluator.isAdminOfCongress(authenticatedUser, congressCode)) {
            throw new UserDisabledException(
                    "Solo los administradores del congreso o súper administradores pueden evaluar comprobantes de pago."
            );
        }

        //4. Validar institución del administrador en el congreso
        validateCurrentUserInstitution(congressCode);

        //5. Buscar el registro del pago
        PaperPayment payment = getPaymentEntityByPaperId(paper.getPaperId(), paperCode);

        //6. REGLA DE NEGOCIO: Si el pago ya fue APROBADO, no se puede cambiar su estado
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            throw new ArgumentNotValidException(
                    "El comprobante de pago ya fue APROBADO previamente y no se puede modificar su estado."
            );
        }

        //7. Regla de negocio: Si se rechaza, la observación es obligatoria
        if (updateDTO.getStatus() == PaymentStatus.REJECTED
                && (updateDTO.getObservations() == null
                || updateDTO.getObservations().trim().isEmpty())) {

            throw new ArgumentNotValidException(
                    "Debe ingresar una observación explicando el motivo del rechazo del comprobante."
            );
        }

        
        //8. Determinar si el comprobante está pasando a estado REJECTED por primera vez.
        boolean shouldNotifyRejectedPayment =
                payment.getStatus() != PaymentStatus.REJECTED
                && updateDTO.getStatus() == PaymentStatus.REJECTED;

        //9. Setear status y observations.
        payment.setStatus(updateDTO.getStatus());
        payment.setObservations(updateDTO.getObservations());

        //10. Sincronizar el flag de pago en la entidad Paper si fue aprobado y capturar la fecha de verificación
        LocalDateTime verificationDate = null;

        if (updateDTO.getStatus() == PaymentStatus.APPROVED) {
            paper.setPay(true);
            verificationDate = DateUtils.now();
        }

        //11. Guardar cambios
        PaperPayment updatedPayment = paymentRepository.save(payment);

        //12. Si fue aprobado, notificar a todos los autores aceptados (PRE-M08).
        if (verificationDate != null) {
            notifyAuthorsAboutVerifiedPayment(
                    updatedPayment,
                    verificationDate
            );
        }

        //13. Si fue rechazado, notificar a todos los autores aceptados (PRE-M09).
        if (shouldNotifyRejectedPayment) {
            notifyAuthorsAboutRejectedPayment(updatedPayment);
        }

        //13. Retornar DTO
        return paymentMapper.toResponseDTO(updatedPayment);
    }

    ///PRIVADOS
    private PaperAuthor getPaperAuthorByPaperIdAndUserId(Long paperId, Long userId) {
        return paperAuthorRepository.findByPaper_PaperIdAndAuthor_UserId(paperId, userId)
                .orElseThrow(() -> new UserDisabledException("El usuario no forma parte de los autores de este trabajo."));
    }

    /**
     * Valida si el usuario tiene acceso de lectura al comprobante.
     * Permitido para ADMINISTRATOR globalmente, o EXPOSITOR si pertenece al grupo de autores, siempre que tengan institución definida.
     */
    private void validateReadAccess(User user, Paper paper) {

        String congressCode = paper.getCongress().getCode();

        boolean isAdmin = securityEvaluator.isAdminOfCongress(user, congressCode);

        // 1. Si no es administrador, debe ser Expositor en el congreso
        if (!isAdmin && !securityEvaluator.isExpositorOfCongress(user, congressCode)) {
            throw new UserDisabledException("Solo los usuarios con rol EXPOSITOR pueden ver comprobantes de pago.");
        }

        // 2. Si no es administrador, debe ser un autor aceptado. Una solicitud
        // PENDING no otorga acceso a documentación financiera del trabajo.
        if (!isAdmin && !securityEvaluator.isAcceptedAuthor(user, paper)) {
            throw new UserDisabledException("Solo los autores aceptados de este trabajo pueden ver el comprobante de pago.");
        }

        // 3. Validar institución del usuario en el congreso
        validateCurrentUserInstitution(congressCode);
    }

    /**
     * Busca la entidad de pago asociada al trabajo o lanza 404 si no existe.
     */
    private PaperPayment getPaymentEntityByPaperId(Long paperId, String paperCode) {
        return paymentRepository.findByPaper_PaperId(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún comprobante registrado para el trabajo: " + paperCode));
    }
    
    private void validateCurrentUserInstitution(String congressCode) {
        institutionValidator.validateAuthenticatedParticipantInstitution(
                congressCode
        );
    }
    

	private void notifyAdministratorsAboutReceivedPayment(PaperPayment payment) {
	
	    notifyAdministrators(
	            payment,
	            "ADM-M02",
	            emailService::sendPaymentReceiptReceivedNotification
	    );
	}
	
	private void notifyAdministratorsAboutRejectedReceiptReplacement(PaperPayment payment) {
	
	    notifyAdministrators(
	            payment,
	            "ADM-M03",
	            emailService::sendPaymentReceiptReplacedAfterRejectionNotification
	    );
	}
	
	private void notifyAdministrators(PaperPayment payment, String notificationCode, BiConsumer<User, PaperPayment> notificationSender) {
	
	    String congressCode = payment.getPaper().getCongress().getCode();
	
	    try {
	
	        List<CongressParticipant> administrators =
	                congressParticipantRepository
	                        .findActiveEnabledParticipantsByCongressCodeAndRole(
	                                congressCode,
	                                RoleName.ADMINISTRATOR
	                        );
	
	        for (CongressParticipant administrator : administrators) {
	
	            try {
	
	                notificationSender.accept(
	                        administrator.getUser(),
	                        payment
	                );
	
	            } catch (RuntimeException exception) {
	
	                log.error(
	                        "No se pudo preparar la notificación {} para el administrador {} del trabajo {}.",
	                        notificationCode,
	                        administrator.getUser().getCode(),
	                        payment.getPaper().getCode(),
	                        exception
	                );
	            }
	        }
	
	    } catch (RuntimeException exception) {
	
	        log.error(
	                "No se pudieron obtener los administradores para la notificación {} del trabajo {}.",
	                notificationCode,
	                payment.getPaper().getCode(),
	                exception
	        );
	    }
	}
	
	private void notifyAuthorsAboutReceivedPayment(PaperPayment payment) {

	    Paper paper = payment.getPaper();

	    // Obtener todos los autores que aceptaron participar del trabajo.
	    List<PaperAuthor> acceptedAuthors = paper.getAuthors().stream()
	            .filter(paperAuthor -> paperAuthor.getStatus() == PaperAuthorStatus.ACCEPTED)
	            .toList();

	    if (acceptedAuthors.isEmpty()) {
	        return;
	    }

	    // Enviar una notificación individual a cada autor aceptado.
	    for (PaperAuthor paperAuthor : acceptedAuthors) {

	        User recipient = paperAuthor.getAuthor();

	        try {

	            emailService.sendPaymentReceiptReceivedToAuthorNotification(
	                    recipient,
	                    payment
	            );

	        } catch (RuntimeException exception) {

	            log.error(
	                    "No se pudo preparar la notificación PRE-M07 para el autor {} del trabajo {}.",
	                    recipient.getCode(),
	                    paper.getCode(),
	                    exception
	            );
	        }
	    }
	}
	
	private void notifyAuthorsAboutVerifiedPayment(PaperPayment payment, LocalDateTime verificationDate) {

	    Paper paper = payment.getPaper();

	    // Obtener todos los autores que aceptaron participar del trabajo.
	    List<PaperAuthor> acceptedAuthors = paper.getAuthors().stream()
	            .filter(paperAuthor -> paperAuthor.getStatus() == PaperAuthorStatus.ACCEPTED)
	            .toList();

	    if (acceptedAuthors.isEmpty()) {
	        return;
	    }

	    // Enviar una notificación individual a cada autor aceptado.
	    for (PaperAuthor paperAuthor : acceptedAuthors) {

	        User recipient = paperAuthor.getAuthor();

	        try {

	            emailService.sendPaymentVerifiedToAuthorNotification(
	                    recipient,
	                    payment,
	                    verificationDate
	            );

	        } catch (RuntimeException exception) {

	            log.error(
	                    "No se pudo preparar la notificación PRE-M08 para el autor {} del trabajo {}.",
	                    recipient.getCode(),
	                    paper.getCode(),
	                    exception
	            );
	        }
	    }
	}
	
	private void notifyAuthorsAboutRejectedPayment(PaperPayment payment) {

	    Paper paper = payment.getPaper();

	    // Obtener todos los autores que aceptaron participar del trabajo.
	    List<PaperAuthor> acceptedAuthors = paper.getAuthors().stream()
	            .filter(paperAuthor -> paperAuthor.getStatus() == PaperAuthorStatus.ACCEPTED)
	            .toList();

	    if (acceptedAuthors.isEmpty()) {
	        return;
	    }

	    // Enviar una notificación individual a cada autor aceptado.
	    for (PaperAuthor paperAuthor : acceptedAuthors) {

	        User recipient = paperAuthor.getAuthor();

	        try {

	            emailService.sendPaymentReceiptRejectedToAuthorNotification(
	                    recipient,
	                    payment
	            );

	        } catch (RuntimeException exception) {

	            log.error(
	                    "No se pudo preparar la notificación PRE-M09 para el autor {} del trabajo {}.",
	                    recipient.getCode(),
	                    paper.getCode(),
	                    exception
	            );
	        }
	    }
	}
}
