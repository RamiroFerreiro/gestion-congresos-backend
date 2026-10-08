package com.tfi.gestion_congresos_backend.security;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.repository.CongressParticipantRepository;
import com.tfi.gestion_congresos_backend.repository.PaperAuthorRepository;

import lombok.RequiredArgsConstructor;

@Component("securityEvaluator")
@RequiredArgsConstructor
public class SecurityEvaluator {

    private final CongressParticipantRepository participantRepository;
    private final PaperAuthorRepository paperAuthorRepository;

    /**
     * Evalúa si el usuario es Súper Admin o cumple con una condición contextual específica.
     * Gracias a la evaluación de cortocircuito (short-circuit), si es Súper Admin 
     * no se ejecuta la consulta a la BD.
     */
    public boolean isSuperAdminOr(User user, Supplier<Boolean> contextualCondition) {
        if (user != null && user.isSuperAdmin()) {
            return true;
        }
        return contextualCondition != null && Boolean.TRUE.equals(contextualCondition.get());
    }

    public boolean isSuperAdmin(User user) {
        return user != null && user.isSuperAdmin();
    }

    /**
     * Método genérico para evaluar cualquier rol en un congreso.
     */
    public boolean hasRoleInCongress(User user, String congressCode, RoleName role) {
        
        return participantRepository.hasRoleInCongress(
                user.getUserId(), 
                congressCode, 
                role
        );
    }

    /**
     * Método genérico para evaluar la participacion de un usuario en un congreso.
     */
    public boolean isParticipantOfCongress(User user, String congressCode) {

        if (isSuperAdmin(user)){
            return true;
        }
        
        return participantRepository.isUserParticipantOfCongress(user.getUserId(), congressCode);
    }

    ///Metodo para evaluar si es Admin en un congreso
    public boolean isAdminOfCongress(User user, String congressCode){

        if (isSuperAdmin(user)){
            return true;
        }

        return hasRoleInCongress(user, congressCode, RoleName.ADMINISTRATOR);
    }

    ///Metodo para evaluar si es Admin en un congreso
    public boolean isExpositorOfCongress(User user, String congressCode){

        if (isSuperAdmin(user)){
            return true;
        }

        return hasRoleInCongress(user, congressCode, RoleName.EXPOSITOR);
    }

    public boolean isParticipantOfPaper(Long paperId, Long userId) {

        if (paperId == null || userId == null) {
            return false;
        }

        return paperAuthorRepository.existsByPaper_PaperIdAndAuthor_UserId(paperId, userId);
    }

}
