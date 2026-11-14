package com.tfi.gestion_congresos_backend.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import com.tfi.gestion_congresos_backend.dtos.abstractbook.AbstractBookAuthorDTO;
import com.tfi.gestion_congresos_backend.dtos.abstractbook.AbstractBookPaperDTO;
import com.tfi.gestion_congresos_backend.entities.Congress;

public final class AbstractBookWordGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    private static final String CONTENT_TYPES = XML_HEADER + """
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml"
                ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
            """;
    private static final String RELATIONSHIPS = XML_HEADER + """
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1"
                Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                Target="word/document.xml"/>
            </Relationships>
            """;

    private AbstractBookWordGenerator() {
    }

    public static byte[] generate(Congress congress, List<AbstractBookPaperDTO> papers) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream archive = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            StringBuilder document = new StringBuilder(XML_HEADER)
                    .append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");
            addCover(document, congress, papers.size());
            if (papers.isEmpty()) {
                addParagraph(document,
                        "No hay trabajos que cumplan las condiciones para incorporarse al Libro de Resúmenes.",
                        "center", 12, false);
            } else {
                for (int index = 0; index < papers.size(); index++) {
                    if (index > 0) {
                        addPageBreak(document);
                    }
                    addPaper(document, papers.get(index));
                }
            }
            document.append("""
                    <w:sectPr>
                      <w:pgSz w:w="11906" w:h="16838"/>
                      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"
                        w:header="720" w:footer="720" w:gutter="0"/>
                    </w:sectPr></w:body></w:document>
                    """);
            writeEntry(archive, "[Content_Types].xml", CONTENT_TYPES);
            writeEntry(archive, "_rels/.rels", RELATIONSHIPS);
            writeEntry(archive, "word/document.xml", document.toString());
            archive.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el Libro de Resúmenes.", exception);
        }
    }

    private static void writeEntry(ZipOutputStream archive, String name, String content) throws IOException {
        archive.putNextEntry(new ZipEntry(name));
        archive.write(content.getBytes(StandardCharsets.UTF_8));
        archive.closeEntry();
    }

    private static void addCover(StringBuilder document, Congress congress, int paperCount) {
        addParagraph(document, "LIBRO DE RESÚMENES", "center", 22, true);
        addParagraph(document, congress.getName(), "center", 16, true);
        addParagraph(document, congress.getPlace(), "center", 12, false);
        addParagraph(document, formatDate(congress.getStartDate()) + " - " + formatDate(congress.getEndDate()),
                "center", 12, false);
        addParagraph(document, "Trabajos incluidos: " + paperCount, "center", 11, false);
        addPageBreak(document);
    }

    private static void addPaper(StringBuilder document, AbstractBookPaperDTO paper) throws IOException {
        addLabeledField(document, "Código", paper.getCode());
        addLabeledField(document, "Título", paper.getTitle());
        addLabeledField(document, "Área temática", paper.getThematicArea());
        addParagraph(document, "Autores", "left", 12, true);
        if (paper.getAuthors() == null || paper.getAuthors().isEmpty()) {
            addParagraph(document, "Sin autores confirmados.", "left", 12, false);
        } else {
            int index = 1;
            for (AbstractBookAuthorDTO author : paper.getAuthors()) {
                String fullName = joinNonBlank(" ", author.getFirstName(), author.getLastName());
                String details = joinNonBlank(", ", fullName, author.getInstitution(), author.getEmail());
                addParagraph(document, index++ + ". " + details, "left", 12, false);
            }
        }
        addParagraph(document, "Palabras clave", "left", 12, true);
        String keywords = paper.getKeywords() == null || paper.getKeywords().isEmpty()
                ? "Sin palabras clave." : String.join("; ", paper.getKeywords());
        addParagraph(document, keywords, "left", 12, false);
        addParagraph(document, "Resumen", "left", 12, true);
        SummaryWriter summaryWriter = new SummaryWriter(document);
        new ParserDelegator().parse(new StringReader(paper.getSummary() == null ? "" : paper.getSummary()),
                summaryWriter, true);
        summaryWriter.closeParagraph();
    }

    private static String joinNonBlank(String delimiter, String... values) {
        return Stream.of(values).filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(delimiter));
    }

    private static void addLabeledField(StringBuilder document, String label, String value) {
        openParagraph(document, "left");
        addRun(document, label + ": ", 12, true, false, false, null);
        addRun(document, value, 12, false, false, false, null);
        document.append("</w:p>");
    }

    private static void addParagraph(StringBuilder document, String text, String alignment, int size, boolean bold) {
        openParagraph(document, alignment);
        addRun(document, text, size, bold, false, false, null);
        document.append("</w:p>");
    }

    private static void openParagraph(StringBuilder document, String alignment) {
        document.append("<w:p><w:pPr><w:spacing w:after=\"120\"/><w:jc w:val=\"")
                .append(alignment).append("\"/></w:pPr>");
    }

    private static void addRun(StringBuilder document, String text, int size, boolean bold,
            boolean italic, boolean underline, String verticalAlignment) {
        document.append("<w:r><w:rPr><w:rFonts w:ascii=\"Times New Roman\" w:hAnsi=\"Times New Roman\"/>")
                .append("<w:sz w:val=\"").append(size * 2).append("\"/>");
        if (bold) {
            document.append("<w:b/>");
        }
        if (italic) {
            document.append("<w:i/>");
        }
        if (underline) {
            document.append("<w:u w:val=\"single\"/>");
        }
        if (verticalAlignment != null) {
            document.append("<w:vertAlign w:val=\"").append(verticalAlignment).append("\"/>");
        }
        document.append("</w:rPr>");
        String[] lines = (text == null ? "" : text).split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                document.append("<w:br/>");
            }
            document.append("<w:t xml:space=\"preserve\">").append(escapeXml(lines[index])).append("</w:t>");
        }
        document.append("</w:r>");
    }

    private static void addPageBreak(StringBuilder document) {
        document.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>");
    }

    private static String escapeXml(String value) {
        StringBuilder escaped = new StringBuilder();
        value.codePoints().filter(codePoint -> codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF))
                .forEach(codePoint -> {
                    switch (codePoint) {
                        case '&' -> escaped.append("&amp;");
                        case '<' -> escaped.append("&lt;");
                        case '>' -> escaped.append("&gt;");
                        case '"' -> escaped.append("&quot;");
                        case '\'' -> escaped.append("&apos;");
                        default -> escaped.appendCodePoint(codePoint);
                    }
                });
        return escaped.toString();
    }

    private static String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_FORMAT.format(dateTime);
    }

    private static final class SummaryWriter extends HTMLEditorKit.ParserCallback {

        private final StringBuilder document;
        private final Map<String, Integer> formats = new HashMap<>();
        private boolean paragraphOpen;

        private SummaryWriter(StringBuilder document) {
            this.document = document;
        }

        @Override
        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (tag == HTML.Tag.P || tag == HTML.Tag.BLOCKQUOTE) {
                closeParagraph();
            }
            formats.merge(tag.toString(), 1, Integer::sum);
        }

        @Override
        public void handleEndTag(HTML.Tag tag, int position) {
            formats.computeIfPresent(tag.toString(), (name, depth) -> depth > 1 ? depth - 1 : null);
            if (tag == HTML.Tag.P || tag == HTML.Tag.BLOCKQUOTE) {
                closeParagraph();
            }
        }

        @Override
        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (tag == HTML.Tag.BR) {
                ensureParagraph();
                document.append("<w:r><w:br/></w:r>");
            }
        }

        @Override
        public void handleText(char[] data, int position) {
            ensureParagraph();
            String verticalAlignment = formats.containsKey("sup") ? "superscript"
                    : formats.containsKey("sub") ? "subscript" : null;
            addRun(document, new String(data), 12,
                    formats.containsKey("b") || formats.containsKey("strong"),
                    formats.containsKey("i") || formats.containsKey("em"),
                    formats.containsKey("u"), verticalAlignment);
        }

        private void ensureParagraph() {
            if (!paragraphOpen) {
                openParagraph(document, "both");
                paragraphOpen = true;
            }
        }

        private void closeParagraph() {
            if (paragraphOpen) {
                document.append("</w:p>");
                paragraphOpen = false;
            }
        }
    }
}
