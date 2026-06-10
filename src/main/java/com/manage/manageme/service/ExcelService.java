package com.manage.manageme.service;

import com.manage.manageme.dto.IncomeDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelService {

    public ByteArrayInputStream generateIncomeExcelReport(List<IncomeDTO> incomes) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inflows Summary Matrix");

            // Header Font Configuration
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Header Row Design Style Layout
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Table Headings Columns Matrix
            String[] columns = {"ID", "Source Title", "Category Name", "Log Date", "Amount ($)"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Explicit Numeric Formatting Layout Style for Currency
            CellStyle decimalCurrencyStyle = workbook.createCellStyle();
            decimalCurrencyStyle.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));

            // Populating Data Matrix Records Rows Loop
            int dynamicRowTracker = 1;
            for (IncomeDTO income : incomes) {
                Row row = sheet.createRow(dynamicRowTracker++);
                row.createCell(0).setCellValue(income.getId() != null ? income.getId() : 0);
                row.createCell(1).setCellValue(income.getName());
                row.createCell(2).setCellValue(income.getCategoryName() != null ? income.getCategoryName() : "Uncategorized");
                row.createCell(3).setCellValue(income.getDate() != null ? income.getDate().toString() : "");

                Cell amountValueCell = row.createCell(4);
                amountValueCell.setCellValue(income.getAmount() != null ? income.getAmount().doubleValue() : 0.00);
                amountValueCell.setCellStyle(decimalCurrencyStyle);
            }

            // Auto-adjust layout column dimensions matching characters spacing sizes
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to construct Excel tracking document structures: " + e.getMessage(), e);
        }
    }
}