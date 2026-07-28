package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.LoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.LoginResponseDTO;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO request);
    
}
