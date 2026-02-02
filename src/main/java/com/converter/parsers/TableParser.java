package com.converter.parsers;

import com.converter.model.Currency;
import com.converter.model.OperationType;
import com.converter.model.TableRow;
import com.converter.model.TheadRow;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableParser {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private final Pattern numberPattern = Pattern.compile("\\d+(\\.\\d+)?");
    private static TableParser instance = new TableParser();
    // Список операций не содержащих название токена
    private final Set<String> operationsWithoutTokens = Set.of(
            "Получение дохода по реферальной программе",
            "Пополнение кошелька",
            "Вывод денежных средств"
    );

    private TableParser() {}

    public static TableParser getInstance() {
        return instance;
    }

    public TheadRow parseThead(Document doc) {
        // 1. Ищем строку заголовков по специфическому классу MUI
        Element headerRow = doc.selectFirst("tr.MuiTableRow-head");

        if (headerRow == null) {
            throw new RuntimeException("Не удалось найти строку заголовков MuiTableRow-head");
        }

        // 2. Извлекаем ячейки th
        List<String> headers = headerRow.select("th").stream()
                .map(th -> {
                    Element firstDiv = th.selectFirst("div.header-sticky-column");

                    if (firstDiv != null) {
                        return firstDiv.text().trim();
                    }
                    return "";
                })
                .filter(text -> !text.isEmpty())
                .toList();

        return new TheadRow(headers);
    }

    public List<TableRow> parseTbody(Document doc) {
        List<TableRow> tableRows = new ArrayList<>();
        // Выбираем все строки tr внутри tbody
        Elements rows = doc.select("tbody tr");

        for (Element row : rows) {
            Elements cells = row.select("td");

            // Пропускаем пустые или технические строки (минимум 5 колонок)
            if (cells.size() < 5) continue;

            // Извлекаем текст из первой доступной оболочки (защита от MUI дублей)
            String opType = getSingleText(cells.get(0));

            // Фильтрация операций
//            if (operationsWithoutTokens.contains(opType)) {
//                continue;
//            }

            String tName = StringUtils.EMPTY;
            try {
                tName = OperationType.getValueOf(opType).isOperationWithoutTokens() ? StringUtils.EMPTY : getSingleText(cells.get(1));
                String tAmountRaw = getSingleText(cells.get(2));
                String priceRaw = getSingleText(cells.get(3));
                String dateRaw = getSingleText(cells.get(4));

                // 1. Парсинг количества токенов (берем только первое число из строки)
                String amountStr = extractFirstNumber(tAmountRaw);
                Integer tAmount = amountStr.isEmpty() ? 0 : (int) Math.round(Double.parseDouble(amountStr));

                // 2. Определение валюты
                Currency curr = Currency.UNKNOWN;
                if (priceRaw.contains("USD")) curr = Currency.USD;
                else if (priceRaw.contains("BYN")) curr = Currency.BYN;

                // 3. Парсинг цены (устранение multiple points через RegEx)
                String priceStr = extractFirstNumber(priceRaw);
                Double priceValue = priceStr.isEmpty() ? 0.0 : Double.parseDouble(priceStr);

                // 4. Парсинг даты
                Date dateValue = dateFormat.parse(dateRaw);

                tableRows.add(new TableRow(opType, tName, tAmount, priceValue, dateValue, curr));

            } catch (Exception e) {
                // Логируем ошибку, чтобы видеть на каком этапе произошел сбой
                System.out.printf("Row [%s] | [%s] skipped. Reason: %s%n", opType, tName, e.getMessage());
            }
        }
        return tableRows;
    }

    /**
     * Извлекает только ПЕРВОЕ встреченное число (целое или дробное).
     * Это решает проблему склеенных строк вида "741.00741.00" -> "741.00"
     */
    private String extractFirstNumber(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // Убираем пробелы и меняем запятую на точку для Double.parseDouble
        String normalized = raw.replace(",", ".").replace(" ", "");
        Matcher matcher = numberPattern.matcher(normalized);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    private String getSingleText(Element cell) {
        // Берем первый встречный span или div, игнорируя дублирующие "sticky" блоки
        Element firstContainer = cell.selectFirst("span, div");
        return (firstContainer != null) ? firstContainer.text().trim() : cell.text().trim();
    }
}
