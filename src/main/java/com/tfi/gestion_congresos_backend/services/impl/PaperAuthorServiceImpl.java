package com.tfi.gestion_congresos_backend.services.impl;

import com.tfi.gestion_congresos_backend.dtos.paper.PaperAuthorResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.paper.PaperAuthorUpdateRequestDTO;
import com.tfi.gestion_congresos_backend.entities.*;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceAlreadyExistsException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.mapper.PaperAuthorMapper;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.PaperAuthorRepository;
import com.tfi.gestion_congresos_backend.repository.PaperRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;
import com.tfi.gestion_congresos_backend.services.CongressParticipantService;
import com.tfi.gestion_congresos_backend.services.EmailService;
import com.tfi.gestion_congresos_backend.services.PaperAuthorService;
import com.tfi.gestion_congresos_backend.services.UserService;
import com.tfi.gestion_congresos_backend.services.validation.CongressParticipantInstitutionValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperAuthorServiceImpl implements PaperAuthorService {

    private final PaperAuthorRepository paperAuthorRepository;
    private final PaperRepository paperRepository; // Injectamos el repo para evitar dependencias circulares con PaperService
    private final CongressParticipantRepository congressParticipantRepository;
    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final CongressParticipantService congressParticipantService;
    private final CongressParticipantInstitutionValidator institutionValidator;
    private final SecurityEvaluator securityEvaluator;
    private final PaperAuthorMapper paperAuthorMapper;
    private final EmailService emailService;

    ///----------------------------------------------------------GETS----------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public List<PaperAuthorResponseDTO> getPaperAuthors(String paperCode) {
        Paper paper = getPaperEntity(paperCode);
        User currentUser = authorizationService.getAuthenticatedUserEntity();

        boolean isSuperAdmin = currentUser.isSuperAdmin();
        boolean isAdmin = securityEvaluator.hasRoleInCongress(currentUser, paper.getCongress().getCode(), RoleName.ADMINISTRATOR);
        boolean isAcceptedAuthor = paperAuthorRepository
                .existsByPaper_CodeAndAuthor_UserIdAndStatus(
                        paperCode,
                        currentUser.getUserId(),
                        PaperAuthorStatus.ACCEPTED
                );

        if (!isSuperAdmin && !isAdmin && !isAcceptedAuthor) {
            throw new AccessDeniedException("No posee permisos para ver los autores de este trabajo.");
        }
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper);

        return paperAuthorMapper.toAcceptedPaperAuthorResponseDTOList(
                paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperAuthorResponseDTO> getCongressAuthors(String congressCode) {
        User currentUser = authorizationService.getAuthenticatedUserEntity();

        if (!currentUser.isSuperAdmin() && !securityEvaluator.hasRoleInCongress(currentUser, congressCode, RoleName.ADMINISTRATOR)) {
            throw new AccessDeniedException("Se requieren permisos de administración en el congreso para ver esta lista.");
        }
        
        // Validar institución del usuario en el congreso
        institutionValidator.validateAuthenticatedParticipantInstitution(congressCode);

        return paperAuthorMapper.toAcceptedPaperAuthorResponseDTOList(
                paperAuthorRepository.findByPaper_Congress_Code(congressCode)
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PaperAuthorResponseDTO> getPendingAuthorRequests(String paperCode) {
        Paper paper = getPaperEntity(paperCode);

        User currentUser = authorizationService.getAuthenticatedUserEntity();

        // Solo el autor responsable, administradores o superadministradores
        // pueden consultar las solicitudes pendientes.
        verifyAdminOrMainAuthorAccess(paper, currentUser);

        List<PaperAuthor> pendingRequests = paperAuthorRepository
                .findByPaper_CodeAndStatus(paperCode, PaperAuthorStatus.PENDING);

        return paperAuthorMapper.toPaperAuthorResponseDTOList(pendingRequests);
    }

    ///----------------------------------------------------------ACTUALIZACIÓN EN LOTE (BULK)----------------------------------------------------------///

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> updatePaperAuthors(
            String paperCode,
            List<PaperAuthorUpdateRequestDTO> updates
    ) {
        Paper paper = getPaperEntity(paperCode);

        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        User currentUser = authorizationService.getAuthenticatedUserEntity();

        boolean isSuperAdmin = currentUser.isSuperAdmin();
        boolean isAdmin = securityEvaluator.hasRoleInCongress(
                currentUser,
                paper.getCongress().getCode(),
                RoleName.ADMINISTRATOR
        );

        List<PaperAuthor> currentAuthors =
                paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode);

        List<PaperAuthor> acceptedAuthors = currentAuthors.stream()
                .filter(author -> author.getStatus() == PaperAuthorStatus.ACCEPTED)
                .toList();

        // Verificar si el usuario actual es el Main Author actual
        boolean isCurrentMainAuthor = acceptedAuthors.stream()
                .anyMatch(author ->
                        author.getAuthor().getUserId().equals(currentUser.getUserId())
                                && author.isMainAuthor()
                );

        // Control de Acceso Global para el método
        if (!isSuperAdmin && !isAdmin && !isCurrentMainAuthor) {
            throw new AccessDeniedException(
                    "No posee permisos para modificar los autores de este trabajo."
            );
        }

        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper);

        // Validación de Integridad: Debe haber exactamente un Main Author
        if (acceptedAuthors.stream().filter(PaperAuthor::isMainAuthor).count() != 1) {
            throw new ArgumentNotValidException(
                    "El trabajo debe tener exactamente un autor principal."
            );
        }

        if (updates == null || updates.size() != acceptedAuthors.size()) {
            throw new ArgumentNotValidException(
                    "Debe enviar exactamente todos los autores aceptados del trabajo."
            );
        }

        Set<String> updatedUserCodes = new HashSet<>();
        Set<Long> requestedOrders = new HashSet<>();

        // Validar integridad de los datos recibidos
        for (PaperAuthorUpdateRequestDTO update : updates) {

            if (update == null
                    || update.getUserCode() == null
                    || update.getAuthorOrder() == null
                    || update.getAuthorOrder() < 1
                    || update.getIsPresentator() == null) {

                throw new ArgumentNotValidException(
                        "Todos los autores deben tener código, orden positivo e indicador de presentador."
                );
            }

            if (!updatedUserCodes.add(update.getUserCode())) {
                throw new ArgumentNotValidException(
                        "La solicitud contiene autores duplicados."
                );
            }

            boolean authorExists = acceptedAuthors.stream()
                    .anyMatch(author ->
                            author.getAuthor().getCode().equals(update.getUserCode())
                    );

            if (!authorExists) {
                throw new ResourceNotFoundException(
                        "El usuario con código " + update.getUserCode()
                                + " no es un autor aceptado de este trabajo."
                );
            }

            if (!requestedOrders.add(update.getAuthorOrder())) {
                throw new ArgumentNotValidException(
                        "No puede haber dos autores con el mismo orden de aparición."
                );
            }
        }

        // Validar que los órdenes formen la secuencia 1, 2, 3... N
        for (long expectedOrder = 1; expectedOrder <= acceptedAuthors.size(); expectedOrder++) {
            if (!requestedOrders.contains(expectedOrder)) {
                throw new ArgumentNotValidException(
                        "El orden de los autores debe ser consecutivo y comenzar en 1."
                );
            }
        }
        
    	// Validar que el autor principal ocupe siempre la primera posición
        String mainAuthorCode = acceptedAuthors.stream()
                .filter(PaperAuthor::isMainAuthor)
                .map(author -> author.getAuthor().getCode())
                .findFirst()
                .orElseThrow(() -> new ArgumentNotValidException(
                        "El trabajo debe tener un autor principal."
                ));

        boolean mainAuthorIsFirst = updates.stream()
                .anyMatch(update ->
                        update.getUserCode().equals(mainAuthorCode)
                                && Long.valueOf(1L).equals(update.getAuthorOrder())
                );

        if (!mainAuthorIsFirst) {
            throw new ArgumentNotValidException(
                    "El autor principal debe ocupar siempre la primera posición (authorOrder = 1)."
            );
        }

        Map<String, Boolean> presenterStatusBeforeUpdate = acceptedAuthors.stream()
                .collect(java.util.stream.Collectors.toMap(
                        author -> author.getAuthor().getCode(),
                        PaperAuthor::isPresentator
                ));

        // Aplicar las modificaciones
        for (PaperAuthorUpdateRequestDTO update : updates) {

            PaperAuthor existingAuthor = acceptedAuthors.stream()
                    .filter(author ->
                            author.getAuthor().getCode().equals(update.getUserCode())
                    )
                    .findFirst()
                    .orElseThrow();

            // Setear nuevos valores
            existingAuthor.setPresentator(update.getIsPresentator());
            existingAuthor.setAuthorOrder(update.getAuthorOrder());
        }

        List<PaperAuthor> savedAuthors = paperAuthorRepository.saveAll(acceptedAuthors);

        for (PaperAuthor author : savedAuthors) {
            boolean wasPresenter = presenterStatusBeforeUpdate.get(author.getAuthor().getCode());

            if (wasPresenter != author.isPresentator()) {
                try {
                    emailService.sendPaperPresenterChangedNotification(
                            author.getAuthor(),
                            paper,
                            author.isPresentator()
                    );
                } catch (RuntimeException exception) {
                    log.error(
                            "No se pudo preparar la notificación de cambio de presentador para el autor {} del trabajo {}.",
                            author.getAuthor().getCode(),
                            paperCode,
                            exception
                    );
                }
            }
        }

        // Ordenamos en memoria para devolver la respuesta limpia al front
        savedAuthors.sort(Comparator.comparing(PaperAuthor::getAuthorOrder));

        return paperAuthorMapper.toAcceptedPaperAuthorResponseDTOList(savedAuthors);
    }
    
    @Override
    @Transactional
    public PaperAuthorResponseDTO requestToJoinPaper(String paperCode, String userCode) {
        Paper paper = getPaperEntity(paperCode);

        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        // Validar que el usuario que solicita sea el mismo que está logueado
        User currentUser = authorizationService.getAuthenticatedUserEntity();

        if (!currentUser.getCode().equals(userCode)) {
            throw new AccessDeniedException(
                    "No puedes solicitar unirte en nombre de otro usuario."
            );
        }

        // Validar participación activa y rol permitido
        validateParticipantCanBeAuthor(currentUser, paper.getCongress());

        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper);

        if (paperAuthorRepository.existsByPaper_CodeAndAuthor_UserId(
                paperCode,
                currentUser.getUserId()
        )) {
            throw new ResourceAlreadyExistsException(
                    "Ya tienes una relación (pendiente o aceptada) con este trabajo."
            );
        }

        PaperAuthor request = PaperAuthor.builder()
                .paper(paper)
                .author(currentUser)
                .isMainAuthor(false)
                .isPresentator(false)
                .status(PaperAuthorStatus.PENDING)
                .build();

        PaperAuthor savedRequest = paperAuthorRepository.save(request);

        try {
            PaperAuthor mainAuthor = paperAuthorRepository
                    .findByPaper_CodeAndIsMainAuthorTrue(paperCode)
                    .filter(author -> author.getStatus() == PaperAuthorStatus.ACCEPTED)
                    .orElseThrow(() -> new IllegalStateException(
                            "El trabajo no tiene un autor principal aceptado."
                    ));

            emailService.sendNewAuthorJoinRequestNotification(
                    mainAuthor.getAuthor(),
                    paper,
                    currentUser
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo preparar la notificación de nueva solicitud de coautoría del usuario {} para el trabajo {}.",
                    currentUser.getCode(),
                    paperCode,
                    exception
            );
        }

        return paperAuthorMapper.toPaperAuthorResponseDTO(savedRequest);
    }

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> acceptAuthorRequest(String paperCode, String userCode) {
        Paper paper = getPaperEntity(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);
        verifyAdminOrMainAuthorAccess(paper, authorizationService.getAuthenticatedUserEntity());

        PaperAuthor request = paperAuthorRepository.findByPaper_CodeAndAuthor_Code(paperCode, userCode)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna solicitud para el usuario indicado."));

        if (request.getStatus() != PaperAuthorStatus.PENDING) {
            throw new ArgumentNotValidException("El usuario ya se encuentra aceptado en este trabajo.");
        }

        validateParticipantCanBeAuthor(
                request.getAuthor(),
                paper.getCongress()
        );
        
        long acceptedCount = paperAuthorRepository.countByPaper_CodeAndStatus(paperCode, PaperAuthorStatus.ACCEPTED);
        validateMaxAuthors(paper.getCongress(), acceptedCount + 1);
        
        Long nextAuthorOrder = paperAuthorRepository
                .findByPaper_CodeAndStatus(paperCode, PaperAuthorStatus.ACCEPTED)
                .stream()
                .map(PaperAuthor::getAuthorOrder)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;

        request.setStatus(PaperAuthorStatus.ACCEPTED);
        request.setAuthorOrder(nextAuthorOrder);

        // Al ser aceptado, si el usuario era LISTENER, se le asciende automáticamente a EXPOSITOR
        boolean promotedToExpositor = congressParticipantService.promoteListenerToExpositor(
                paper.getCongress().getCode(),
                request.getAuthor().getCode()
        );

        paperAuthorRepository.save(request);

        try {
            emailService.sendAuthorJoinRequestAcceptedNotification(
                    request.getAuthor(),
                    paper,
                    promotedToExpositor
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo preparar la notificación de aceptación de coautoría para el usuario {} del trabajo {}.",
                    request.getAuthor().getCode(),
                    paperCode,
                    exception
            );
        }

        return paperAuthorMapper.toAcceptedPaperAuthorResponseDTOList(
                paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode)
        );
    }

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> rejectAuthorRequest(String paperCode, String userCode) {
        Paper paper = getPaperEntity(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);
        verifyAdminOrMainAuthorAccess(paper, authorizationService.getAuthenticatedUserEntity());

        PaperAuthor request = paperAuthorRepository.findByPaper_CodeAndAuthor_Code(paperCode, userCode)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna solicitud para el usuario indicado."));

        if (request.getStatus() != PaperAuthorStatus.PENDING) {
            throw new ArgumentNotValidException("Solo se pueden rechazar solicitudes en estado PENDING.");
        }

        paperAuthorRepository.delete(request);

        try {
            emailService.sendAuthorJoinRequestRejectedNotification(
                    request.getAuthor(),
                    paper
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo preparar la notificación de rechazo de coautoría para el usuario {} del trabajo {}.",
                    request.getAuthor().getCode(),
                    paperCode,
                    exception
            );
        }

        return paperAuthorMapper.toAcceptedPaperAuthorResponseDTOList(
                paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode)
        );
    }

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> addAuthorDirectly(String paperCode, String userCode) {
        Paper paper = getPaperEntity(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);
        verifyAdminAccess(paper, authorizationService.getAuthenticatedUserEntity()); // SOLO Admin o SuperAdmin

        User userToAdd = userService.getUserByUserCode(userCode);
        validateParticipantCanBeAuthor(userToAdd, paper.getCongress());

        if (paperAuthorRepository.existsByPaper_CodeAndAuthor_UserId(paperCode, userToAdd.getUserId())) {
            throw new ResourceAlreadyExistsException("El usuario ya tiene una relación con este trabajo.");
        }

        long acceptedCount = paperAuthorRepository.countByPaper_CodeAndStatus(paperCode, PaperAuthorStatus.ACCEPTED);
        validateMaxAuthors(paper.getCongress(), acceptedCount + 1);
        
        Long nextAuthorOrder = paperAuthorRepository
                .findByPaper_CodeAndStatus(paperCode, PaperAuthorStatus.ACCEPTED)
                .stream()
                .map(PaperAuthor::getAuthorOrder)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;

        PaperAuthor newAuthor = PaperAuthor.builder()
                .paper(paper)
                .author(userToAdd)
                .authorOrder(nextAuthorOrder)
                .isMainAuthor(false)
                .isPresentator(false)
                .status(PaperAuthorStatus.ACCEPTED)
                .build();

        boolean promotedToExpositor = congressParticipantService.promoteListenerToExpositor(
                paper.getCongress().getCode(),
                userToAdd.getCode()
        );
        paperAuthorRepository.save(newAuthor);

        try {
            emailService.sendAuthorAddedByAdministratorNotification(
                    userToAdd,
                    paper,
                    promotedToExpositor
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo preparar la notificación de incorporación administrativa para el usuario {} del trabajo {}.",
                    userToAdd.getCode(),
                    paperCode,
                    exception
            );
        }

        try {
            PaperAuthor mainAuthor = paperAuthorRepository
                    .findByPaper_CodeAndIsMainAuthorTrue(paperCode)
                    .filter(author -> author.getStatus() == PaperAuthorStatus.ACCEPTED)
                    .orElseThrow(() -> new IllegalStateException(
                            "El trabajo no tiene un autor principal aceptado."
                    ));

            emailService.sendAuthorAddedByAdministratorToMainAuthorNotification(
                    mainAuthor.getAuthor(),
                    paper,
                    userToAdd
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo preparar la notificación al autor principal por la incorporación administrativa del usuario {} al trabajo {}.",
                    userToAdd.getCode(),
                    paperCode,
                    exception
            );
        }

        return paperAuthorMapper.toAcceptedPaperAuthorResponseDTOList(
                paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode)
        );
    }

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> removeAuthorFromPaper(
            String paperCode,
            String userCode
    ) {
        Paper paper = getPaperEntity(paperCode);

        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        verifyAdminOrMainAuthorAccess(
                paper,
                authorizationService.getAuthenticatedUserEntity()
        );

        PaperAuthor authorToRemove = paperAuthorRepository
                .findByPaper_CodeAndAuthor_Code(paperCode, userCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no tiene relación con este Paper."
                ));

        if (authorToRemove.isMainAuthor()) {
            throw new ArgumentNotValidException(
                    "No se puede eliminar al autor principal. Debe transferir la autoría antes de eliminarlo."
            );
        }

        if (authorToRemove.getStatus() != PaperAuthorStatus.ACCEPTED) {
            throw new ArgumentNotValidException(
                    "Solo se pueden eliminar autores en estado ACCEPTED."
            );
        }

        List<PaperAuthor> currentAuthors =
                paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode);

        // Obtener los autores aceptados restantes
        List<PaperAuthor> remainingAcceptedAuthors = currentAuthors.stream()
                .filter(author -> author.getStatus() == PaperAuthorStatus.ACCEPTED)
                .filter(author -> !author.getAuthor().getCode().equals(userCode))
                .toList();

        if (remainingAcceptedAuthors.isEmpty()) {
            throw new ArgumentNotValidException(
                    "El trabajo debe conservar al menos un autor aceptado."
            );
        }

        // Verificar que exista exactamente un autor principal
        List<PaperAuthor> mainAuthors = remainingAcceptedAuthors.stream()
                .filter(PaperAuthor::isMainAuthor)
                .toList();

        if (mainAuthors.size() != 1) {
            throw new ArgumentNotValidException(
                    "El trabajo debe tener exactamente un autor principal."
            );
        }

        // Verificar que los autores restantes tengan un orden asignado
        if (remainingAcceptedAuthors.stream()
                .anyMatch(author -> author.getAuthorOrder() == null)) {
            throw new ArgumentNotValidException(
                    "Todos los autores aceptados deben tener un orden asignado."
            );
        }

        PaperAuthor mainAuthor = mainAuthors.get(0);

        // El autor principal siempre ocupa la primera posición
        mainAuthor.setAuthorOrder(1L);

        // Ordenar los demás autores respetando su posición relativa
        List<PaperAuthor> otherAuthors = remainingAcceptedAuthors.stream()
                .filter(author -> !author.isMainAuthor())
                .sorted(Comparator.comparing(PaperAuthor::getAuthorOrder))
                .toList();

        long newOrder = 2L;

        for (PaperAuthor author : otherAuthors) {
            author.setAuthorOrder(newOrder++);
        }

        // Eliminar el autor solicitado
        paperAuthorRepository.delete(authorToRemove);

        // Guardar los órdenes actualizados
        paperAuthorRepository.saveAll(remainingAcceptedAuthors);

        try {
            emailService.sendAuthorRemovedFromPaperNotification(
                    authorToRemove.getAuthor(),
                    paper
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo preparar la notificación de eliminación de coautoría para el usuario {} del trabajo {}.",
                    authorToRemove.getAuthor().getCode(),
                    paperCode,
                    exception
            );
        }

        return paperAuthorMapper.toAcceptedPaperAuthorResponseDTOList(
                paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode)
        );
    }
    
    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> transferMainAuthor(
            String paperCode,
            String newMainAuthorCode
    ) {
        Paper paper = getPaperEntity(paperCode);

        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        User currentUser = authorizationService.getAuthenticatedUserEntity();

        List<PaperAuthor> currentAuthors =
                paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode);

        // Verificar que exista exactamente un autor principal
        List<PaperAuthor> mainAuthors = currentAuthors.stream()
                .filter(PaperAuthor::isMainAuthor)
                .toList();

        if (mainAuthors.size() != 1
                || mainAuthors.get(0).getStatus() != PaperAuthorStatus.ACCEPTED) {
            throw new ArgumentNotValidException(
                    "El trabajo debe tener exactamente un autor principal aceptado."
            );
        }

        PaperAuthor currentMainAuthor = mainAuthors.get(0);

        // Verificar permisos del usuario autenticado
        boolean isSuperAdmin = currentUser.isSuperAdmin();

        boolean isAdmin = securityEvaluator.hasRoleInCongress(
                currentUser,
                paper.getCongress().getCode(),
                RoleName.ADMINISTRATOR
        );

        boolean isCurrentMainAuthor = currentMainAuthor.getAuthor()
                .getUserId()
                .equals(currentUser.getUserId());

        if (!isSuperAdmin && !isAdmin && !isCurrentMainAuthor) {
            throw new AccessDeniedException(
                    "No posee permisos para transferir la autoría principal de este trabajo."
            );
        }

        validateCurrentUserInstitution(paper);

        // Buscar al nuevo responsable
        PaperAuthor newMainAuthor = currentAuthors.stream()
                .filter(author -> author.getAuthor().getCode().equals(newMainAuthorCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario indicado no es autor de este trabajo."
                ));

        if (newMainAuthor.getStatus() != PaperAuthorStatus.ACCEPTED) {
            throw new ArgumentNotValidException(
                    "El nuevo responsable debe ser un autor aceptado del trabajo."
            );
        }

        if (newMainAuthor.isMainAuthor()) {
            throw new ArgumentNotValidException(
                    "El usuario indicado ya es el autor principal del trabajo."
            );
        }
        
        validateParticipantCanBeAuthor(
                newMainAuthor.getAuthor(),
                paper.getCongress()
        );

        // Obtener los autores aceptados y verificar que todos tengan un orden asignado
        List<PaperAuthor> acceptedAuthors = currentAuthors.stream()
                .filter(author -> author.getStatus() == PaperAuthorStatus.ACCEPTED)
                .toList();

        if (acceptedAuthors.stream().anyMatch(author -> author.getAuthorOrder() == null)) {
            throw new ArgumentNotValidException(
                    "Todos los autores aceptados deben tener un orden de autoría asignado."
            );
        }

        // Obtener los demás autores respetando su orden actual
        List<PaperAuthor> remainingAuthors = acceptedAuthors.stream()
                .filter(author -> !author.getAuthor().getCode().equals(newMainAuthorCode))
                .sorted(Comparator.comparing(PaperAuthor::getAuthorOrder))
                .toList();

        // Transferir la autoría principal
        currentMainAuthor.setMainAuthor(false);
        newMainAuthor.setMainAuthor(true);

        // El nuevo autor principal siempre ocupa la primera posición
        newMainAuthor.setAuthorOrder(1L);

        // Reasignar consecutivamente las posiciones de los demás autores
        long nextAuthorOrder = 2L;

        for (PaperAuthor author : remainingAuthors) {
            author.setAuthorOrder(nextAuthorOrder++);
        }

        // Guardar los cambios de todos los autores aceptados
        paperAuthorRepository.saveAll(acceptedAuthors);

        try {
            emailService.sendMainAuthorTransferredNotification(
                    currentMainAuthor.getAuthor(),
                    paper,
                    false
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo preparar la notificación de transferencia de autoría para el autor principal anterior {} del trabajo {}.",
                    currentMainAuthor.getAuthor().getCode(),
                    paperCode,
                    exception
            );
        }

        try {
            emailService.sendMainAuthorTransferredNotification(
                    newMainAuthor.getAuthor(),
                    paper,
                    true
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo preparar la notificación de transferencia de autoría para el nuevo autor principal {} del trabajo {}.",
                    newMainAuthor.getAuthor().getCode(),
                    paperCode,
                    exception
            );
        }

        // Devolver únicamente los autores aceptados
        List<PaperAuthor> updatedAuthors =
                paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode);

        return paperAuthorMapper.toAcceptedPaperAuthorResponseDTOList(updatedAuthors);
    }

    ///----------------------------------------------------------MÉTODOS PRIVADOS AUXILIARES----------------------------------------------------------///

    private void verifyAdminOrMainAuthorAccess(Paper paper, User currentUser) {
        boolean isSuperAdmin = currentUser.isSuperAdmin();
        boolean isAdmin = securityEvaluator.hasRoleInCongress(currentUser, paper.getCongress().getCode(), RoleName.ADMINISTRATOR);
        
        boolean isMainAuthor = paperAuthorRepository.findByPaper_CodeAndIsMainAuthorTrue(paper.getCode())
                .map(pa -> pa.getAuthor().getUserId().equals(currentUser.getUserId()))
                .orElse(false);

        if (!isSuperAdmin && !isAdmin && !isMainAuthor) {
            throw new AccessDeniedException("Acceso denegado. Se requiere ser Autor Principal del trabajo o Administrador del congreso.");
        }
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper);
    }

    private void verifyAdminAccess(Paper paper, User currentUser) {
        if (!currentUser.isSuperAdmin() && !securityEvaluator.hasRoleInCongress(currentUser, paper.getCongress().getCode(), RoleName.ADMINISTRATOR)) {
            throw new AccessDeniedException("Acceso denegado. Operación exclusiva para Administradores del congreso.");
        }
        
        // Validar institución del usuario en el congreso
        validateCurrentUserInstitution(paper);
    }

    private void validateParticipantCanBeAuthor(User user, Congress congress) {
    	if (!user.isEnabled()) {
    	    throw new AccessDeniedException(
    	            "La cuenta del usuario está deshabilitada y no puede ser autor de trabajos."
    	    );
    	}
    	
    	if (user.isSuperAdmin()) {
            throw new AccessDeniedException(
                    "Los superadministradores no pueden ser autores de trabajos."
            );
        }

        CongressParticipant participant = congressParticipantRepository
                .findActiveParticipantByUserCodeAndCongressCode(
                        user.getCode(),
                        congress.getCode()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no está inscripto activamente en este congreso."
                ));

        RoleName roleName = participant.getRole().getName();

        if (roleName != RoleName.LISTENER && roleName != RoleName.EXPOSITOR) {
            throw new AccessDeniedException(
                    "Solo los participantes con rol LISTENER o EXPOSITOR pueden ser autores de un trabajo."
            );
        }
    }

    private void validateMaxAuthors(Congress congress, long totalAuthors) {
        if (congress.getMaxNumberOfAuthors() != null && totalAuthors > congress.getMaxNumberOfAuthors()) {
            throw new ArgumentNotValidException("La operación supera el máximo de autores permitidos por el congreso.");
        }
    }

    private Paper getPaperEntity(String paperCode) {
        return paperRepository.findByCode(paperCode)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo no encontrado con código: " + paperCode));
    }

    private void validatePaperEditableState(Paper paper, PaperStatus... allowedStates) {
        boolean isAllowed = java.util.Arrays.stream(allowedStates).anyMatch(s -> s == paper.getStatus());
        if (!isAllowed) {
            throw new ArgumentNotValidException("No se puede realizar esta acción con el Paper en estado " + paper.getStatus());
        }
    }
    
    // Validar institución del usuario en el congreso
    private void validateCurrentUserInstitution(Paper paper) {
        institutionValidator.validateAuthenticatedParticipantInstitution(
                paper.getCongress().getCode()
        );
    }
}
