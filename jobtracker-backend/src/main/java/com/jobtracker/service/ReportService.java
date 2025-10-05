package com.jobtracker.service;

import com.jobtracker.model.Application;
import com.jobtracker.repository.ApplicationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private final ApplicationRepository applicationRepository;

    public ReportService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    // Excel Export
    public ByteArrayInputStream generateExcel(String status) throws Exception {
        List<Application> applications = (status != null) ?
                applicationRepository.findByUserId(status) :
                applicationRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Applications");
            Row header = sheet.createRow(0);

            String[] cols = {"Company", "Job Title", "Status", "Applied Date", "Job Location"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            int rowIdx = 1;
            for (Application app : applications) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(app.getCompanyName());
                row.createCell(1).setCellValue(app.getJobTitle());
                row.createCell(2).setCellValue(app.getStatus().toString());
                row.createCell(3).setCellValue(app.getAppliedDate() != null ?
                        app.getAppliedDate().format(DateTimeFormatter.ISO_DATE) : "");
                row.createCell(4).setCellValue(app.getJobLocation());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // PDF Export
    public ByteArrayInputStream generatePdf(String status) {
    List<Application> applications = (status != null) ?
            applicationRepository.findByUserId(status) :
            applicationRepository.findAll();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PdfWriter writer = new PdfWriter(out);
    PdfDocument pdfDoc = new PdfDocument(writer);
    Document document = new Document(pdfDoc);

    // Title
    document.add(new Paragraph("Job Applications Report")
            .setBold()
            .setFontSize(16)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20));

    // Define a table with 4 columns
    float[] columnWidths = {200F, 200F, 120F, 150F};
    Table table = new Table(columnWidths);
    table.setWidth(UnitValue.createPercentValue(100));

    // Header style
    DeviceRgb headerBg = new DeviceRgb(200, 200, 200);

    table.addHeaderCell(new Cell()
            .add(new Paragraph("Company").setBold())
            .setBackgroundColor(headerBg)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5));

    table.addHeaderCell(new Cell()
            .add(new Paragraph("Job Title").setBold())
            .setBackgroundColor(headerBg)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5));

    table.addHeaderCell(new Cell()
            .add(new Paragraph("Status").setBold())
            .setBackgroundColor(headerBg)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5));

    table.addHeaderCell(new Cell()
            .add(new Paragraph("Applied On").setBold())
            .setBackgroundColor(headerBg)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5));

    // Data rows
    for (Application app : applications) {
        table.addCell(new Cell()
                .add(new Paragraph(app.getCompanyName()))
                .setPadding(5));

        table.addCell(new Cell()
                .add(new Paragraph(app.getJobTitle()))
                .setPadding(5));

        table.addCell(new Cell()
                .add(new Paragraph(app.getStatus().toString()))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5));

        table.addCell(new Cell()
                .add(new Paragraph(
                        app.getAppliedDate() != null ? app.getAppliedDate().toString() : "-"
                ))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5));
    }

    document.add(table);
    document.close();
    return new ByteArrayInputStream(out.toByteArray());
}

}
