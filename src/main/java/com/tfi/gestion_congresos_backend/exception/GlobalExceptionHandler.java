package com.tfi.gestion_congresos_backend.exception;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.security.access.AccessDeniedException;
import com.tfi.gestion_congresos_backend.dtos.ErrorResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /// 400 - Argumento o lógica de negocio inválida:
    @ExceptionHandler(ArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleArgumentNotValid(ArgumentNotValidException ex) {
    	
    	ErrorResponseDTO error = ErrorResponseDTO.builder()
    			.timestamp(LocalDateTime.now())
    			.status(HttpStatus.BAD_REQUEST.value())
    			.error("Bad Request")
    			.message(ex.getMessage())
    			.build();
    	
    	// Creamos las cabeceras forzando explícitamente APPLICATION_JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
    }
    
    /// 400 - Fallo de validación de Bean Validation (@Valid en los DTOs):
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    	
    	String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
    	
    	ErrorResponseDTO error = ErrorResponseDTO.builder()
    			.timestamp(LocalDateTime.now())
    			.status(HttpStatus.BAD_REQUEST.value())
    			.error("Bad Request")
    			.message(errorMessage)
    			.build();
    	
    	// Creamos las cabeceras forzando explícitamente APPLICATION_JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
    }
    
    /// 400 - Tipo de dato incorrecto en parámetros de URL (@PathVariable o @RequestParam):
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        
        // Obtenemos el nombre del tipo de dato esperado de forma limpia:
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido";

        // Armamos el mensaje:
        String customMessage = String.format(
            "El parámetro '%s' con el valor '%s' es inválido. Se esperaba un dato de tipo %s.",
            ex.getName(), 
            ex.getValue(), 
            requiredType
        );

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(customMessage)
                .build();

        // Creamos las cabeceras forzando explícitamente APPLICATION_JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
    }
    
    /// 401 - No se pudo completar la autenticación:
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(InvalidCredentialsException ex){

    	ErrorResponseDTO error = ErrorResponseDTO.builder()
    			.timestamp(LocalDateTime.now())
    			.status(HttpStatus.UNAUTHORIZED.value())
    			.error("Unauthorized")
    			.message(ex.getMessage())
    			.build();
    	
        // Creamos las cabeceras forzando explícitamente APPLICATION_JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
    } 
    
    /// 403 - El usuario no cuenta con los permisos necesarios:
    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserDisabled(UserDisabledException ex){
    	
    	ErrorResponseDTO error = ErrorResponseDTO.builder()
    			.timestamp(LocalDateTime.now())
    			.status(HttpStatus.FORBIDDEN.value())
    			.error("Forbidden")
    			.message(ex.getMessage())
    			.build();
    	
        // Creamos las cabeceras forzando explícitamente APPLICATION_JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
    } 

    /// 403 - Acceso denegado por Spring Security (@PreAuthorize):
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex) {

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("No tienes los permisos necesarios para acceder a este recurso.")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.FORBIDDEN);
    }
    
    /// 404 - Recurso no encontrado:
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex) {

    	ErrorResponseDTO error = ErrorResponseDTO.builder()
    			.timestamp(LocalDateTime.now())
    			.status(HttpStatus.NOT_FOUND.value())
    			.error("Not Found")
    			.message(ex.getMessage())
    			.build();
    	
        // Creamos las cabeceras forzando explícitamente APPLICATION_JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
    }

    /// 409 - El recurso ya existe:
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDTO> handleResourceAlreadyExists(ResourceAlreadyExistsException ex){
    	
    	ErrorResponseDTO error = ErrorResponseDTO.builder()
    			.timestamp(LocalDateTime.now())
    			.status(HttpStatus.CONFLICT.value())
    			.error("Conflict")
    			.message(ex.getMessage())
    			.build();
    	
        // Creamos las cabeceras forzando explícitamente APPLICATION_JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
    }
    
    /// 500 - Error no controlado del servidor:
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(Exception ex) {
    	
    	ErrorResponseDTO error = ErrorResponseDTO.builder()
    			.timestamp(LocalDateTime.now())
    			.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
    			.error("Internal Server Error")
    			.message("Ocurrió un error inesperado en el servidor.")
    			.build();
    	
    	// Creamos las cabeceras forzando explícitamente APPLICATION_JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(error, headers, HttpStatus.NOT_FOUND);
    }
}
