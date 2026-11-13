package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.exception.InvalidCredentialsException;
import com.tfi.gestion_congresos_backend.exception.UserDisabledException;
import com.tfi.gestion_congresos_backend.security.SecurityEvaluator;
import com.tfi.gestion_congresos_backend.services.AuthorizationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

    private final SecurityEvaluator securityEvaluator;

    /// Obtener el usuario autenticado:
    @Override
    public User getAuthenticatedUserEntity() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {

            throw new InvalidCredentialsException("Usuario no autenticado");
        }

        return user;
    }

    /// Validar permiso de súper administrador:
    @Override
    public void validateSuperAdminPermission(User user) {

        if (user == null || !user.isEnabled() || !securityEvaluator.isSuperAdmin(user)) {
            throw new UserDisabledException(
                    "Solo un súper administrador puede realizar esta operación."
            );
        }
    }

    /// Validar permiso de administrador de un congreso:
    @Override
    public void validateCongressAdminPermission(User user, String congressCode) {

        if (user == null || !user.isEnabled()
                || !securityEvaluator.isAdminOfCongress(user, congressCode)) {
            throw new UserDisabledException(
                    "No tienes permisos de administración sobre el congreso: " + congressCode
            );
        }
    }
}
