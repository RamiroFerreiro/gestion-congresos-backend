package com.tfi.gestion_congresos_backend.exception;

public class UserDisabledException extends RuntimeException{
    public UserDisabledException(String message){
        super(message);
    }
}
