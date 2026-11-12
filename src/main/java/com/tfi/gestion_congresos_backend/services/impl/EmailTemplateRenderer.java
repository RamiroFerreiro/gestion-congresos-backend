package com.tfi.gestion_congresos_backend.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

@Component
public class EmailTemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{([a-zA-Z0-9_.-]+)}}");

    public String render(String templatePath, Map<String, String> variables) {

        Objects.requireNonNull(templatePath, "La ruta de la plantilla no puede ser null.");
        Objects.requireNonNull(variables, "Las variables no pueden ser null.");

        String template = loadTemplate(templatePath);

        Set<String> placeholders = extractPlaceholders(template);

        // Verificar que se hayan recibido todas las variables necesarias.
        Set<String> missingVariables = new HashSet<>(placeholders);
        missingVariables.removeAll(variables.keySet());

        // Detectar variables enviadas que no existen en la plantilla.
        Set<String> unexpectedVariables = new HashSet<>(variables.keySet());
        unexpectedVariables.removeAll(placeholders);

        if (!missingVariables.isEmpty() || !unexpectedVariables.isEmpty()) {
            throw new IllegalArgumentException(
                    "Variables incorrectas para la plantilla '" + templatePath +
                    "'. Faltantes: " + missingVariables +
                    ". Inesperadas: " + unexpectedVariables
            );
        }

        // Reemplazar cada placeholder escapando su contenido HTML.
        for (Map.Entry<String, String> entry : variables.entrySet()) {

            String value = Objects.requireNonNull(
                    entry.getValue(),
                    "El valor de la variable '" + entry.getKey() + "' no puede ser null."
            );

            String placeholder = "{{" + entry.getKey() + "}}";

            template = template.replace(
                    placeholder,
                    HtmlUtils.htmlEscape(value)
            );
        }

        return template;
    }

    private Set<String> extractPlaceholders(String template) {

        Set<String> placeholders = new HashSet<>();

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }

        return placeholders;
    }

    private String loadTemplate(String path) {

        ClassPathResource resource = new ClassPathResource(path);

        try (InputStream inputStream = resource.getInputStream()) {

            return StreamUtils.copyToString(
                    inputStream,
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "No se pudo cargar la plantilla de email: " + path,
                    e
            );
        }
    }
}
