package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tfi.gestion_congresos_backend.services.FileStorageService;
import java.io.IOException;
import java.nio.file.*;

@Service
public class FileStorageServiceImpl implements FileStorageService {

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
            Path destinationFile = this.rootLocation.resolve(Paths.get(newFileName)).normalize().toAbsolutePath();

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
    
    
}
