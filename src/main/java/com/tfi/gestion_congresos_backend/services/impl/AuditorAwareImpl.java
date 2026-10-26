package com.tfi.gestion_congresos_backend.services.impl;


import com.tfi.gestion_congresos_backend.entities.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of("SYSTEM");
        }

        // Como tu Principal es directamente la entidad User:
        if (authentication.getPrincipal() instanceof User user) {
            return Optional.ofNullable(user.getEmail());
        }

        return Optional.ofNullable(authentication.getName());
    }
}