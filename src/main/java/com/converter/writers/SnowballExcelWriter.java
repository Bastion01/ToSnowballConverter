package com.converter.writers;

import com.converter.model.TableRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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

                // Обработка "Покупка токенов" -> Cash_In и Buy
                if ("Покупка токенов".equals(tr.operationType())) {
                    rowNum = createSnowballRow(sheet, rowNum, tr, "Buy");
                    rowNum = createSnowballRow(sheet, rowNum, tr, "Cash_In");
                }
                // Обработка "Получение дохода" -> Dividend
                else if (Arrays.asList("Получение дохода", "Получение дохода по реферальной программе").contains(tr.operationType())) {
                    rowNum = createSnowballRow(sheet, rowNum, tr, "Dividend");
                }
                // Игнорируем другие операции, которые не мапятся
                else {
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
    private int createSnowballRow(Sheet sheet, int rowNum, TableRow tr, String eventType) {
        Row row = sheet.createRow(rowNum++);

        // Col 0: Event
        row.createCell(0).setCellValue(eventType);

        // Col 1: Date (формат yyyy-MM-dd)
        row.createCell(1).setCellValue(dateFormat.format(tr.date()));

        // Col 2: Symbol
        row.createCell(2).setCellValue(tr.tokenName());

        // Col 3: Price
        double priceValue = 0.0;
        if ("Cash_In".equals(eventType)) {
            priceValue = 1.0;
        } else if ("Buy".equals(eventType)) {
            // Защита от деления на ноль
            priceValue = (tr.tokenAmount() > 0) ? tr.price() / tr.tokenAmount() : 0.0;
        } else if ("Dividend".equals(eventType)) {
            priceValue = 999.0; // Согласно вашему ТЗ
        }
        row.createCell(3).setCellValue(Double.parseDouble(priceFormat.format(priceValue)));

        // Col 4: Quantity
        double quantityValue = 0.0;
        if ("Buy".equals(eventType)) {
            quantityValue = tr.tokenAmount();
        } else if ("Cash_In".equals(eventType) || "Dividend".equals(eventType)) {
            quantityValue = tr.price();
        }
        row.createCell(4).setCellValue(quantityValue);

        // Col 5: Currency
        row.createCell(5).setCellValue(tr.currency().name());

        // Остальные колонки 6-11 остаются пустыми (по умолчанию пустая ячейка в Excel это ОК)

        return rowNum;
    }
}
