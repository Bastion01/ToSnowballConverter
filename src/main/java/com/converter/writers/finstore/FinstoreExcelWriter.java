package com.converter.writers.finstore;

import com.converter.model.TableRow;
import com.converter.model.TheadRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class FinstoreExcelWriter {
    public static FinstoreExcelWriter instance = new FinstoreExcelWriter();
    private FinstoreExcelWriter() {}

    public void writeReport(TheadRow headers, List<TableRow> data) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Транзакции");

            // 1. Создаем стили
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy hh:mm:ss"));

            // 2. Записываем заголовок (thead)
            Row headerRow = sheet.createRow(0);
            List<String> colNames = headers.columnNames();
            for (int i = 0; i < colNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colNames.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 3. Записываем данные (tbody)
            int rowNum = 1;
            for (TableRow dataRow : data) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(dataRow.operationType());
                row.createCell(1).setCellValue(dataRow.tokenName());
                row.createCell(2).setCellValue(dataRow.tokenAmount());

                // Сумма + Валюта в одной ячейке (или можно разбить)
                row.createCell(3).setCellValue(dataRow.price());
                row.createCell(4).setCellValue(dataRow.currency().name());

                Cell dateCell = row.createCell(5);
                dateCell.setCellValue(dataRow.date());
                dateCell.setCellStyle(dateStyle);
            }

            // 4. Автоподбор ширины колонок
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            // 5. Сохранение
            try (FileOutputStream fileOut = new FileOutputStream("FinstoreReport.xlsx")) {
                workbook.write(fileOut);
            }
            System.out.println("Файл FinstoreReport.xlsx успешно создан!");

        } catch (IOException e) {
            System.err.println("Ошибка при создании Excel файла: " + e.getMessage());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    public static FinstoreExcelWriter getInstance() {
        return instance;
    }
}
