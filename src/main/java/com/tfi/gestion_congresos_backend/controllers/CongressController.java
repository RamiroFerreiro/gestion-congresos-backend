
package com.tfi.gestion_congresos_backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tfi.gestion_congresos_backend.dtos.congress.CongressRequestDTO;
import com.tfi.gestion_congresos_backend.dtos.congress.CongressResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.congress.CongressSummaryDTO;
import com.tfi.gestion_congresos_backend.dtos.dashboard.CongressDashboardResponseDTO;
import com.tfi.gestion_congresos_backend.dtos.user.MessageResponseDTO;
import com.tfi.gestion_congresos_backend.services.CongressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "Congress",
    description = "Operaciones relacionadas con la gestión de congresos"
)
@RestController
@RequestMapping("/api/congresses")
@RequiredArgsConstructor
public class CongressController {

    private final CongressService congressService;

    /// Obtener congresos (todos / activos / desactivados):
    @Operation(
        summary = "Obtener resumen congresos según el estado indicado",
        description = "Obtiene todos los congresos registrados o los que estén activos o desactivados.")
    @ApiResponse(
        responseCode = "200",
        description = "Congresos obtenidos correctamente"
    )
    @PreAuthorize("@securityEvaluator.isSuperAdmin(authentication.principal)")
    @GetMapping
    public ResponseEntity<List<CongressSummaryDTO>> getCongresses(
            @RequestParam(required = false) Boolean enabled) {

        return ResponseEntity.ok(congressService.getCongressesSummaryByEnabled(enabled));
    }

    /// Obtener un congreso por código:
    @Operation(
        summary = "Obtener un congreso por código",
        description = "Obtiene la información de un congreso a partir de su código."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Congreso obtenido correctamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró un congreso con el código especificado"
        )
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{code}")
    public ResponseEntity<CongressResponseDTO> getCongressByCode(
            @PathVariable String code
    ) {

        return ResponseEntity.ok(
            congressService.getCongressDTOByCode(code)
        );
    }

    /// Crear un congreso:
    @Operation(
        summary = "Crear un congreso",
        description = """
                Registra un nuevo congreso y crea automáticamente su único tipo de trabajo ABSTRACT.
                Los congresos pagos deben informar abstractAmount y abstractCurrency. En los gratuitos,
                ambos campos son opcionales y el importe se configura en cero.
                """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Congreso creado correctamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Los datos enviados no son válidos. Las fechas deben respetar los rangos y el orden cronológico establecido, y ser futuras."
        )
    })
    @PreAuthorize("@securityEvaluator.isSuperAdmin(authentication.principal)")
    @PostMapping
    public ResponseEntity<CongressResponseDTO> createCongress(
            @Valid @RequestBody CongressRequestDTO request
    ) {

        CongressResponseDTO created = congressService.createCongress(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /// Desactivar un congreso:
    @Operation(
        summary = "Desactivar un congreso por código",
        description = "Establece como desactivado el congreso."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Congreso desactivado correctamente"
        ),
        @ApiResponse(
    	    responseCode = "401",
    	    description = "Usuario no autenticado"
    	),
    	@ApiResponse(
    	    responseCode = "403",
    	    description = "No posee permisos de administración o debe completar su institución"
    	),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró un congreso con el código especificado"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El congreso ya se encontraba desactivado"
        )
    })
    @PatchMapping("/{code}/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponseDTO> disableCongress(
            @PathVariable String code
    ) {

        return ResponseEntity.ok(
            congressService.disableCongress(code)
        );
    }

    /// Activar un congreso:
    @Operation(
        summary = "Activar un congreso por código",
        description = "Establece como activado el congreso."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Congreso activado correctamente"
        ),
        @ApiResponse(
    	    responseCode = "401",
    	    description = "Usuario no autenticado"
    	),
    	@ApiResponse(
    	    responseCode = "403",
    	    description = "No posee permisos de administración o debe completar su institución"
    	),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró un congreso con el código especificado"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El congreso ya se encontraba activado"
        )
    })
    @PatchMapping("/{code}/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponseDTO> enableCongress(
            @PathVariable String code
    ) {

        return ResponseEntity.ok(
            congressService.enableCongress(code)
        );
    }

    /// Actualizar un congreso:
    @Operation(
        summary = "Actualizar un congreso",
        description = "Modifica un congreso existente en el sistema."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Congreso actualizado correctamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos. Las fechas modificadas deben ser futuras; las fechas pasadas pueden conservarse sin cambios. También deben respetarse los rangos y el orden cronológico."
        ),
        @ApiResponse(
    	    responseCode = "401",
    	    description = "Usuario no autenticado"
    	),
    	@ApiResponse(
    	    responseCode = "403",
    	    description = "No posee permisos de administración o debe completar su institución"
    	),
        @ApiResponse(
            responseCode = "404",
            description = "No se encontró un congreso con el código especificado"
        )
    })
    @PutMapping("/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CongressResponseDTO> updateCongress(
            @PathVariable String code,
            @Valid @RequestBody CongressRequestDTO request
    ) {

        return ResponseEntity.ok(
            congressService.updateCongress(code, request)
        );
    }

    @Operation(
        summary = "Obtener el resumen de métricas del Dashboard",
        description = """
            Devuelve en una única llamada HTTP los indicadores académicos (trabajos recibidos, sin evaluador, en evaluación, etc.) 
            y los indicadores financieros (pagos pendientes y verificados) del congreso especificado.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Métricas del panel obtenidas exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado: El token JWT es inválido o no fue provisto"),
        @ApiResponse(responseCode = "403", description = "Prohibido: El usuario autenticado no posee permisos de administración sobre este congreso"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado: El código de congreso especificado no existe")
    })
    @GetMapping("/{congressCode}/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CongressDashboardResponseDTO> getDashboardMetrics( @PathVariable String congressCode) {

        CongressDashboardResponseDTO dashboardMetrics = congressService.getDashboardMetrics(congressCode);
        return ResponseEntity.ok(dashboardMetrics);
    }
}
