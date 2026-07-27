package com.tfi.gestion_congresos_backend.repository;

import com.tfi.gestion_congresos_backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
