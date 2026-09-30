package com.tfi.gestion_congresos_backend.services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Guarda un archivo asociado a un trabajo en el almacenamiento.
     * @param file Archivo multipart recibido en la petición.
     * @param paperId Identificador del trabajo.
     * @return Ruta o URI donde quedó almacenado el archivo.
     */
    String store(MultipartFile file, Long paperId);
    
    Resource loadAsResource(String filePath);
    void delete(String filePath);
}
