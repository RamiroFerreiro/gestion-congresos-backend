package com.tfi.gestion_congresos_backend.services;

import com.tfi.gestion_congresos_backend.dtos.UserRequest;
import com.tfi.gestion_congresos_backend.dtos.UserResponse;


public interface UserService {

    UserResponse createUser(UserRequest request);

}