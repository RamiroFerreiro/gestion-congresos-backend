package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import com.tfi.gestion_congresos_backend.exception.ResourceNotFoundException;
import com.tfi.gestion_congresos_backend.services.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private final Path rootLocation = Paths.get("uploads/payments");

    public FileStorageServiceImpl() {
        try {
            //Verifica si la carpeta uploads/payments existe. Si no existe, la crea.
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            //Si el sistema operativo deniega permisos de escritura o no se puede crear la carpeta, 
            // lanza una excepción Runtime para detener el arranque con un mensaje claro.
            throw new RuntimeException("No se pudo inicializar la carpeta de almacenamiento", e);
        }
    }

    @Override
    public String store(MultipartFile file, Long paperId) {
        try {
            //Verifica que se haya enviado efectivamente un archivo y que no sea de 0 bytes.
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("El archivo enviado no puede estar vacío");
            }

            //Extrae la extensión (.pdf, .png, etc.)
            String fileExtension = getFileExtension(file.getOriginalFilename());
            //Genera un nombre estandarizado para evitar colisiones cuando varios usuarios suben archivos con el mismo nombre
            String newFileName = "payment_paper_" + paperId + "_" + System.currentTimeMillis() + fileExtension;

            //Construcción de la Ruta Absoluta de Destino
            Path destinationFile = this.rootLocation.resolve(Paths.get(newFileName)).normalize();
            Files.copy(file.getInputStream(), destinationFile.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);       

            //Guardado Físico en Disco
            //Abre el flujo de bytes del archivo enviado en la petición HTTP
            //Copia ese flujo de bytes en el archivo destino en disco
            //Si por algún motivo ya existía un archivo con el mismo nombre exacto, lo sobreescribe sin lanzar error
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            //Retorna la cadena con la ruta absoluta donde quedó guardado el archivo
            //luego se persiste en el campo filePath de la entidad PaperPayment
            return destinationFile.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo en el sistema local", e);
        }
    }

    //Extrae la extensión (.pdf, .png, etc.)
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }
    
    @Override
    public void delete(String filePath) {
        if (filePath != null) {
            try {
                Path path = Paths.get(filePath);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                //Log de advertencia pero sin interrumpir el flujo si el archivo físico ya no existía
                log.warn("No se pudo eliminar el archivo previo en la ruta: {}", filePath);
            }
        }
    }

    @Override
    public Resource loadAsResource(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("El archivo físico no se encuentra disponible en el servidor.");
            }
        } catch (MalformedURLException e) {
            throw new ArgumentNotValidException("Error al procesar la ruta del archivo.");
        }
    }
    
}
