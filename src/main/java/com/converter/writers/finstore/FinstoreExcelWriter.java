package com.converter.writers.finstore;

import com.converter.model.TableRow;
import com.converter.model.TheadRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FinstoreExcelWriter {
    private static final Logger logger = LoggerFactory.getLogger(FinstoreExcelWriter.class);
    public static final String FILENAME = "FinstoreReport.xlsx";
    public static FinstoreExcelWriter instance = new FinstoreExcelWriter();
    private FinstoreExcelWriter() {}

    public File writeReport(TheadRow headers, List<TableRow> data) {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path reportPath = Paths.get(tempDir).resolve(FILENAME);
        File reportFile = reportPath.toFile();

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
                row.createCell(3).setCellValue(String.format("%s %s",dataRow.price(), dataRow.currency().name()));

                Cell dateCell = row.createCell(4);
                dateCell.setCellValue(dataRow.date());
                dateCell.setCellStyle(dateStyle);
            }

            // 4. Автоподбор ширины колонок
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(reportFile)) {
                workbook.write(fileOut);
            }
            logger.debug("File {} successfully created!,", FILENAME);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred during excel file creation: " + e.getMessage(), e);
        }
        return reportFile;
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
