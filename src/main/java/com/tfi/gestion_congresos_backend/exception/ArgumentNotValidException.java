package com.tfi.gestion_congresos_backend.exception;

public class ArgumentNotValidException extends RuntimeException {

	public ArgumentNotValidException(String message){
        super(message);
    }
}
