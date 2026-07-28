package com.tfi.gestion_congresos_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    //404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    //409
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<String> handleEmailAlreadyExists(EmailAlreadyExistsException ex){

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
    
    //401 Unauthorized
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException ex){

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    } 
    
    //403 FORBIDDEN
    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<String> handleUserDisabled(UserDisabledException ex){

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }   


}   
