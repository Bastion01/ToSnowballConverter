package com.converter.writers.snowball;

import com.converter.model.enums.SnowballEventType;
import com.converter.model.enums.FinstoreOperationType;
import com.converter.model.TableRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class SnowballExcelWriter {
    public static SnowballExcelWriter instance = new SnowballExcelWriter();

    private SnowballExcelWriter() {}

    public static SnowballExcelWriter getInstance() {
        return instance;
    }

    // Формат даты для колонки Date (yyyy-mm-dd)
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // Формат для чисел с плавающей точкой
    private final DecimalFormat priceFormat = new DecimalFormat("#.##########");

    public void writeSnowballReport(List<TableRow> data) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Snowball Data");

            // 1. Создаем стили и заголовки Snowball
            List<String> headers = List.of("Event", "Date", "Symbol", "Price", "Quantity", "Currency", "FeeTax", "Exchange", "NKD", "FeeCurrency", "DoNotAdjustCash", "Note");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                // Можно добавить стиль заголовка, как в предыдущем классе, если нужно
            }

            // 2. Записываем данные с учетом маппинга
            int rowNum = 1;
            for (TableRow tr : data) {
                FinstoreOperationType operationType = FinstoreOperationType.getValueOf(tr.operationType());
                if (FinstoreOperationType.CASH_IN.equals(operationType)) {
                    rowNum = createSnowballRow(sheet, rowNum, tr, SnowballEventType.CASH_IN);
                } else if (FinstoreOperationType.BUY_TOKENS.equals(operationType)) {
                    rowNum = createSnowballRow(sheet, rowNum, tr, SnowballEventType.BUY);
                }
                else if (FinstoreOperationType.INCOME.equals(operationType)) {
                    rowNum = createSnowballRow(sheet, rowNum, tr, SnowballEventType.DIVIDEND);
                } else if (FinstoreOperationType.INCOME_REFERRAL.equals(operationType)) {
                    rowNum = createSnowballRow(sheet, rowNum, tr, SnowballEventType.CASH_GAIN);
                } else if (FinstoreOperationType.CASH_OUT.equals(operationType)) {
                    rowNum = createSnowballRow(sheet, rowNum, tr, SnowballEventType.CASH_OUT);
                } else {
                    System.out.println("Ignored operation type for Snowball: " + tr.operationType());
                }
            }

            // 3. Автоподбор ширины колонок
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // 4. Сохранение
            try (FileOutputStream fileOut = new FileOutputStream("FinstoreReport_Snowball.xlsx")) {
                workbook.write(fileOut);
            }
            System.out.println("Файл FinstoreReport_Snowball.xlsx успешно создан!");

        } catch (IOException e) {
            System.err.println("Ошибка при создании Snowball Excel файла: " + e.getMessage());
        }
    }

    /**
     * Вспомогательный метод для создания одной строки в формате Snowball
     */
    private int createSnowballRow(Sheet sheet, int rowNum, TableRow tr, SnowballEventType eventType) {
        Row row = sheet.createRow(rowNum++);

        // Col 0: Event
        row.createCell(0).setCellValue(eventType.getValue());

        // Col 1: Date (формат yyyy-MM-dd)
        row.createCell(1).setCellValue(dateFormat.format(tr.date()));

        // Col 2: Symbol
        row.createCell(2).setCellValue(tr.tokenName());

        // Col 3: Price
        double priceValue = 0.0;
        if (eventType.isCashInOut()) {
            priceValue = 1.0;
        } else if (SnowballEventType.BUY.equals(eventType)) {
            // Защита от деления на ноль
            priceValue = (tr.tokenAmount() > 0) ? tr.price() / tr.tokenAmount() : 0.0;
        } else if (SnowballEventType.DIVIDEND.equals(eventType)) {
            priceValue = 999.0; // Согласно вашему ТЗ
        }
        row.createCell(3).setCellValue(Double.parseDouble(priceFormat.format(priceValue)));

        // Col 4: Quantity
        double quantityValue = 0.0;
        if (SnowballEventType.BUY.equals(eventType)) {
            quantityValue = tr.tokenAmount();
        } else if (eventType.isIncomeEvent()) {
            quantityValue = tr.price();
        }
        row.createCell(4).setCellValue(quantityValue);

        // Col 5: Currency
        row.createCell(5).setCellValue(tr.currency().name());

        // Остальные колонки 6-11 остаются пустыми (по умолчанию пустая ячейка в Excel это ОК)

        return rowNum;
    }
}
