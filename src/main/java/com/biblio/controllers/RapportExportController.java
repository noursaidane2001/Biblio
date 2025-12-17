package com.biblio.controllers;

import com.biblio.dto.StatistiqueDTO;
import com.biblio.services.StatistiquesService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/rapports")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class RapportExportController {
    private final StatistiquesService statistiquesService;

    public RapportExportController(StatistiquesService statistiquesService) {
        this.statistiquesService = statistiquesService;
    }

    @GetMapping("/export/csv")
    public void exportCSV(HttpServletResponse response) throws Exception {
        List<StatistiqueDTO> stats = statistiquesService.getToutesLesStatistiques();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        String filename = "rapports-analytique-" + timestamp + ".csv";

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        response.getOutputStream().write(0xEF); // UTF-8 BOM for Excel
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Date", "Bibliothèque", "Catégorie", "Nombre Prêts", "Taux Rotation")
                .setDelimiter(';')
                .build();

        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (StatistiqueDTO s : stats) {
                printer.printRecord(
                        s.date(),
                        s.bibliotheque(),
                        s.categorie(),
                        s.nombrePrets(),
                        String.format("%.2f%%", s.tauxRotation())
                );
            }
        }
    }

    @GetMapping("/export/pdf")
    public void exportPDF(HttpServletResponse response) throws Exception {
        List<StatistiqueDTO> stats = statistiquesService.getToutesLesStatistiques();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        String filename = "rapports-analytique-" + timestamp + ".pdf";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        var titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        var headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        var cellFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        document.add(new Paragraph("Rapports Analytiques", titleFont));
        document.add(new Paragraph(
                "Taux de rotation global: " + String.format("%.2f%%", statistiquesService.getTauxRotationGlobal())
        ));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{18f, 22f, 22f, 18f, 20f});

        Color green = new Color(24, 119, 62);
        addHeaderCell(table, "Date", headerFont, green);
        addHeaderCell(table, "Bibliothèque", headerFont, green);
        addHeaderCell(table, "Catégorie", headerFont, green);
        addHeaderCell(table, "Nombre Prêts", headerFont, green);
        addHeaderCell(table, "Taux Rotation", headerFont, green);

        for (StatistiqueDTO s : stats) {
            addCell(table, String.valueOf(s.date()), cellFont);
            addCell(table, s.bibliotheque(), cellFont);
            addCell(table, s.categorie(), cellFont);
            addCell(table, String.valueOf(s.nombrePrets()), cellFont);
            addCell(table, String.format("%.2f%%", s.tauxRotation()), cellFont);
        }

        document.add(table);
        document.close();
    }

    private static void addHeaderCell(PdfPTable table, String text, com.lowagie.text.Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private static void addCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5f);
        table.addCell(cell);
    }
}

