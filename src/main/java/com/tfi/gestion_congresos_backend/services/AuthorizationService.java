package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.entities.User;

public interface AuthorizationService {
	User getAuthenticatedUserEntity();

    void validateSuperAdminPermission(User user);

    void validateCongressAdminPermission(User user, String congressCode);
}
