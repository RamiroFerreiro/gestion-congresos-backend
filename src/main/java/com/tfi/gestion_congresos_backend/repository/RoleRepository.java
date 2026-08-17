package com.tfi.gestion_congresos_backend.repository;

import java.util.Optional;
import com.tfi.gestion_congresos_backend.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tfi.gestion_congresos_backend.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role>findByName(RoleName name);
}