package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.auth.ForgotPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.LoginResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.auth.ResetPasswordRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO request);

    MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO request);

    MessageResponseDTO resetPassword(ResetPasswordRequestDTO request);

}
