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
import com.tfi.gestion_congresos_backend.repository.RoleRepository;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.PaperAuthorService;
import com.tfi.gestion_congresos_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaperAuthorServiceImpl implements PaperAuthorService {

    private final PaperAuthorRepository paperAuthorRepository;
    private final PaperRepository paperRepository; // Injectamos el repo para evitar dependencias circulares con PaperService
    private final CongressParticipantRepository congressParticipantRepository;
    private final UserService userService;
    private final SecurityEvaluator securityEvaluator;
    private final PaperAuthorMapper paperAuthorMapper;
    private final RoleRepository roleRepository;

    // TODO_EMAIL: Inyectar ApplicationEventPublisher eventPublisher; para enviar los emails luego.

    ///----------------------------------------------------------GETS----------------------------------------------------------///

    @Override
    @Transactional(readOnly = true)
    public List<PaperAuthorResponseDTO> getPaperAuthors(String paperCode) {
        Paper paper = getPaperEntity(paperCode);
        User currentUser = userService.getAuthenticatedUserEntity();

        boolean isSuperAdmin = currentUser.isSuperAdmin();
        boolean isAdmin = securityEvaluator.hasRoleInCongress(currentUser, paper.getCongress().getCode(), RoleName.ADMINISTRATOR);
        boolean isAuthor = paperAuthorRepository.existsByPaper_CodeAndAuthor_UserId(paperCode, currentUser.getUserId());

        if (!isSuperAdmin && !isAdmin && !isAuthor) {
            throw new AccessDeniedException("No posee permisos para ver los autores de este trabajo.");
        }

        return paperAuthorMapper.toPaperAuthorResponseDTOList(paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperAuthorResponseDTO> getCongressAuthors(String congressCode) {
        User currentUser = userService.getAuthenticatedUserEntity();

        if (!currentUser.isSuperAdmin() && !securityEvaluator.hasRoleInCongress(currentUser, congressCode, RoleName.ADMINISTRATOR)) {
            throw new AccessDeniedException("Se requieren permisos de administración en el congreso para ver esta lista.");
        }

        return paperAuthorMapper.toPaperAuthorResponseDTOList(paperAuthorRepository.findByPaper_Congress_Code(congressCode));
    }

    ///----------------------------------------------------------ACTUALIZACIÓN EN LOTE (BULK)----------------------------------------------------------///

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> updatePaperAuthors(String paperCode, List<PaperAuthorUpdateRequestDTO> updates) {
        Paper paper = getPaperEntity(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED, PaperStatus.NEEDS_REVISION);

        User currentUser = userService.getAuthenticatedUserEntity();
        boolean isSuperAdmin = currentUser.isSuperAdmin();
        boolean isAdmin = securityEvaluator.hasRoleInCongress(currentUser, paper.getCongress().getCode(), RoleName.ADMINISTRATOR);

        List<PaperAuthor> currentAuthors = paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode);
        
        // Verificar si el usuario actual es el Main Author actual
        boolean isCurrentMainAuthor = currentAuthors.stream()
                .anyMatch(pa -> pa.getAuthor().getUserId().equals(currentUser.getUserId()) && pa.isMainAuthor());

        // Control de Acceso Global para el método
        if (!isSuperAdmin && !isAdmin && !isCurrentMainAuthor) {
            throw new AccessDeniedException("No posee permisos para modificar los autores de este trabajo.");
        }

        // Validación de Integridad: Debe haber exactamente un Main Author
        long newMainAuthorCount = updates.stream().filter(PaperAuthorUpdateRequestDTO::getIsMainAuthor).count();
        if (newMainAuthorCount != 1) {
            throw new ArgumentNotValidException("Debe existir exactamente un autor principal (isMainAuthor = true).");
        }

        // Aplicar actualizaciones
        for (PaperAuthorUpdateRequestDTO update : updates) {
            PaperAuthor existingAuthor = currentAuthors.stream()
                    .filter(pa -> pa.getAuthor().getCode().equals(update.getUserCode()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("El usuario con código " + update.getUserCode() + " no es autor de este paper (o no existe)."));

            if (existingAuthor.getStatus() != PaperAuthorStatus.ACCEPTED) {
                throw new ArgumentNotValidException("Solo se pueden modificar atributos de autores en estado ACCEPTED.");
            }

            // REGLA CLAVE: Modificar la autoría principal es exclusivo de Admins/SuperAdmins
            if (existingAuthor.isMainAuthor() != update.getIsMainAuthor()) {
                if (!isSuperAdmin && !isAdmin) {
                    throw new AccessDeniedException("Solo un Administrador del congreso o un Súper Admin puede transferir la autoría principal.");
                }
            }

            // Setear nuevos valores
            existingAuthor.setMainAuthor(update.getIsMainAuthor());
            existingAuthor.setPresentator(update.getIsPresentator());
            existingAuthor.setAuthorOrder(update.getAuthorOrder());
        }

        List<PaperAuthor> savedAuthors = paperAuthorRepository.saveAll(currentAuthors);
        
        // Ordenamos en memoria para devolver la respuesta limpia al front
        savedAuthors.sort(Comparator.comparingLong(PaperAuthor::getAuthorOrder));
        return paperAuthorMapper.toPaperAuthorResponseDTOList(savedAuthors);
    }

  @Override
    @Transactional
    public List<PaperAuthorResponseDTO> requestToJoinPaper(String paperCode, String userCode) {
        Paper paper = getPaperEntity(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);

        // Validar que el usuario que solicita sea el mismo que está logueado (No puedes pedir unirte en nombre de otro)
        User currentUser = userService.getAuthenticatedUserEntity();
        if (!currentUser.getCode().equals(userCode)) {
            throw new AccessDeniedException("No puedes solicitar unirte en nombre de otro usuario.");
        }

        // Validar que participa en el congreso y no es Evaluador
        validateParticipantCanBeAuthor(userCode, paper.getCongress());

        if (paperAuthorRepository.existsByPaper_CodeAndAuthor_UserId(paperCode, currentUser.getUserId())) {
            throw new ResourceAlreadyExistsException("Ya tienes una relación (pendiente o aceptada) con este trabajo.");
        }

        PaperAuthor request = PaperAuthor.builder()
                .paper(paper)
                .author(currentUser)
                .authorOrder(0L) // Orden 0 mientras sea PENDING
                .isMainAuthor(false)
                .isPresentator(false)
                .status(PaperAuthorStatus.PENDING)
                .build();

        paperAuthorRepository.save(request);

        // TODO_EMAIL: Notificar al Main Author que recibió una nueva solicitud de co-autoría

        return paperAuthorMapper.toPaperAuthorResponseDTOList(paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode));
    }

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> acceptAuthorRequest(String paperCode, String userCode) {
        Paper paper = getPaperEntity(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);
        verifyAdminOrMainAuthorAccess(paper, userService.getAuthenticatedUserEntity());

        PaperAuthor request = paperAuthorRepository.findByPaper_CodeAndAuthor_Code(paperCode, userCode)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna solicitud para el usuario indicado."));

        if (request.getStatus() != PaperAuthorStatus.PENDING) {
            throw new ArgumentNotValidException("El usuario ya se encuentra aceptado en este trabajo.");
        }

        long acceptedCount = paperAuthorRepository.countByPaper_CodeAndStatus(paperCode, PaperAuthorStatus.ACCEPTED);
        validateMaxAuthors(paper.getCongress(), acceptedCount + 1);

        request.setStatus(PaperAuthorStatus.ACCEPTED);
        request.setAuthorOrder(acceptedCount + 1);

        // Al ser aceptado, si el usuario era LISTENER, se le asciende automáticamente a EXPOSITOR
        upgradeToExpositorRoleIfNeeded(request.getAuthor(), paper.getCongress());

        paperAuthorRepository.save(request);

        // TODO_EMAIL: Notificar al usuario 'userCode' que su solicitud fue ACEPTADA

        return paperAuthorMapper.toPaperAuthorResponseDTOList(paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode));
    }

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> rejectAuthorRequest(String paperCode, String userCode) {
        Paper paper = getPaperEntity(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);
        verifyAdminOrMainAuthorAccess(paper, userService.getAuthenticatedUserEntity());

        PaperAuthor request = paperAuthorRepository.findByPaper_CodeAndAuthor_Code(paperCode, userCode)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna solicitud para el usuario indicado."));

        if (request.getStatus() != PaperAuthorStatus.PENDING) {
            throw new ArgumentNotValidException("Solo se pueden rechazar solicitudes en estado PENDING.");
        }

        paperAuthorRepository.delete(request);

        // TODO_EMAIL: Notificar al usuario 'userCode' que su solicitud fue RECHAZADA

        return paperAuthorMapper.toPaperAuthorResponseDTOList(paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode));
    }

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> addAuthorDirectly(String paperCode, String userCode) {
        Paper paper = getPaperEntity(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);
        verifyAdminAccess(paper, userService.getAuthenticatedUserEntity()); // SOLO Admin o SuperAdmin

        User userToAdd = userService.getUserByUserCode(userCode);
        validateParticipantCanBeAuthor(userCode, paper.getCongress());

        if (paperAuthorRepository.existsByPaper_CodeAndAuthor_UserId(paperCode, userToAdd.getUserId())) {
            throw new ResourceAlreadyExistsException("El usuario ya tiene una relación con este trabajo.");
        }

        long acceptedCount = paperAuthorRepository.countByPaper_CodeAndStatus(paperCode, PaperAuthorStatus.ACCEPTED);
        validateMaxAuthors(paper.getCongress(), acceptedCount + 1);

        PaperAuthor newAuthor = PaperAuthor.builder()
                .paper(paper)
                .author(userToAdd)
                .authorOrder(acceptedCount + 1)
                .isMainAuthor(false)
                .isPresentator(false)
                .status(PaperAuthorStatus.ACCEPTED)
                .build();

        upgradeToExpositorRoleIfNeeded(userToAdd, paper.getCongress());
        paperAuthorRepository.save(newAuthor);

        // TODO_EMAIL: Notificar al usuario 'userCode' que fue agregado como autor directamente por un administrador

        return paperAuthorMapper.toPaperAuthorResponseDTOList(paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode));
    }

    @Override
    @Transactional
    public List<PaperAuthorResponseDTO> removeAuthorFromPaper(String paperCode, String userCode) {
        Paper paper = getPaperEntity(paperCode);
        validatePaperEditableState(paper, PaperStatus.NOT_SUBMITTED);
        verifyAdminOrMainAuthorAccess(paper, userService.getAuthenticatedUserEntity());

        PaperAuthor authorToRemove = paperAuthorRepository.findByPaper_CodeAndAuthor_Code(paperCode, userCode)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene relación con este Paper."));

        if (authorToRemove.isMainAuthor()) {
            throw new ArgumentNotValidException("No se puede eliminar al autor principal. Debe transferir la autoría antes de eliminarlo.");
        }

        if (authorToRemove.getStatus() == PaperAuthorStatus.PENDING) {
            throw new ArgumentNotValidException("El usuario tiene una solicitud pendiente. Utilice el endpoint de rechazo.");
        }

        // Recuperar la lista actual antes de borrar para reordenar
        List<PaperAuthor> remainingAuthors = paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode);
        remainingAuthors.remove(authorToRemove);
        
        paperAuthorRepository.delete(authorToRemove);

        // Compactar los órdenes (1, 2, 3...) sin dejar huecos
        long newOrder = 1;
        for (PaperAuthor pa : remainingAuthors) {
            if (pa.getStatus() == PaperAuthorStatus.ACCEPTED) {
                pa.setAuthorOrder(newOrder++);
            }
        }
        paperAuthorRepository.saveAll(remainingAuthors);

        // TODO_EMAIL: Notificar al usuario 'userCode' que ha sido removido del trabajo

        return paperAuthorMapper.toPaperAuthorResponseDTOList(paperAuthorRepository.findByPaper_CodeOrderByAuthorOrderAsc(paperCode));
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
    }

    private void verifyAdminAccess(Paper paper, User currentUser) {
        if (!currentUser.isSuperAdmin() && !securityEvaluator.hasRoleInCongress(currentUser, paper.getCongress().getCode(), RoleName.ADMINISTRATOR)) {
            throw new AccessDeniedException("Acceso denegado. Operación exclusiva para Administradores del congreso.");
        }
    }

    private void validateParticipantCanBeAuthor(String userCode, Congress congress) {
        CongressParticipant participant = congressParticipantRepository
                .findUserActiveParticipantOfCongress(userCode, congress.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no está inscripto en este congreso."));

        if (participant.getRole().getName() == RoleName.EVALUATOR) {
            throw new ArgumentNotValidException("Un evaluador del congreso no puede postularse como autor de un trabajo.");
        }
    }

    private void upgradeToExpositorRoleIfNeeded(User user, Congress congress) {
        CongressParticipant participant = congressParticipantRepository
                .findUserActiveParticipantOfCongress(user.getCode(), congress.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no está inscripto en el congreso."));

        if (participant.getRole().getName() == RoleName.LISTENER) {
            Role expositorRole = roleRepository.findByName(RoleName.EXPOSITOR)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol EXPOSITOR no encontrado en el sistema."));
            
            participant.setRole(expositorRole);
            congressParticipantRepository.save(participant);
            
            // TODO_EMAIL: (Opcional) Notificar al usuario que su rol fue ascendido de Oyente a Expositor
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
}