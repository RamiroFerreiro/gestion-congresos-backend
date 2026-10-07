package com.tfi.gestion_congresos_backend.security;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.tfi.gestion_congresos_backend.entities.User;

import lombok.RequiredArgsConstructor;

@Component("securityEvaluator")
@RequiredArgsConstructor
public class SecurityEvaluator {

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

}
