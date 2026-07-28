package com.tfi.gestion_congresos_backend.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
    	
    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
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
    	
    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
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

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
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
    	
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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
    	
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
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
    	
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /// 409 - El recurso ya existe:
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceAlreadyExists(ResourceAlreadyExistsException ex){
    	
    	ErrorResponseDTO error = ErrorResponseDTO.builder()
    			.timestamp(LocalDateTime.now())
    			.status(HttpStatus.CONFLICT.value())
    			.error("Conflict")
    			.message(ex.getMessage())
    			.build();
    	
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    /// 500 - Error no controlado del servidor:
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(Exception ex) {
    	
    	ErrorResponseDTO error = ErrorResponseDTO.builder()
    			.timestamp(LocalDateTime.now())
    			.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
    			.error("Internal Server Error")
    			.message("Ocurrió un error inesperado en el servidor.")
    			.build();
    	
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
