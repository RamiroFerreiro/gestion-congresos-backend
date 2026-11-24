package com.tfi.gestion_congresos_backend.services.validation;

import com.tfi.gestion_congresos_backend.exception.ArgumentNotValidException;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * Sanitiza el HTML admitido para el resumen de un trabajo.
 *
 * <p>La política no habilita atributos: sólo se preservan las etiquetas de
 * formato explícitamente autorizadas.</p>
 */
@Component
public class PaperSummarySanitizer {

    private static final PolicyFactory SUMMARY_HTML_POLICY = new HtmlPolicyBuilder()
            .allowElements(
                    "p",
                    "strong",
                    "b",
                    "em",
                    "i",
                    "u",
                    "sub",
                    "sup",
                    "blockquote",
                    "br"
            )
            .toFactory();

    private static final PolicyFactory TEXT_ONLY_POLICY = new HtmlPolicyBuilder()
            .toFactory();

    public String sanitizeAndValidate(String rawSummary) {
        if (rawSummary == null) {
            throw new ArgumentNotValidException("El resumen es obligatorio.");
        }

        String sanitizedSummary = SUMMARY_HTML_POLICY.sanitize(rawSummary);
        String visibleText = TEXT_ONLY_POLICY.sanitize(sanitizedSummary);

        if (!hasMeaningfulText(visibleText)) {
            throw new ArgumentNotValidException(
                    "El resumen debe contener texto significativo luego de aplicar el formato permitido."
            );
        }

        return sanitizedSummary;
    }

    private boolean hasMeaningfulText(String text) {
        if (text == null) {
            return false;
        }

        String normalizedText = text
                .replace("&nbsp;", "")
                .replace("&#160;", "")
                .replace("&#xA0;", "");

        return normalizedText.codePoints().anyMatch(codePoint ->
                !Character.isWhitespace(codePoint)
                        && !Character.isSpaceChar(codePoint)
                        && codePoint != 0x200B
                        && codePoint != 0x200C
                        && codePoint != 0x200D
                        && codePoint != 0xFEFF
        );
    }
}
