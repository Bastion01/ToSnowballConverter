package com.converter.writers.finstore;

import org.apache.commons.lang3.StringUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FinstoreCategoriesCsvWriter {
    private static final FinstoreCategoriesCsvWriter INSTANCE = new FinstoreCategoriesCsvWriter();

    private static final String FILE_NAME = "SnowballCategories.csv";
    private static final String HEADER = "Parent,PieAsset,IsAsset,IsUnallocated,IsLocked,Allocation";
    private static final String ROW_TEMPLATE = "%s,%s,%b,false,false,\"0,0000\"";

    private FinstoreCategoriesCsvWriter() {}

    public void writeCategories(Set<String> inputTokenNames, boolean isForceCreateNeeded) {
        Path path = Paths.get(FILE_NAME);
        Map<String, String> dataMap = new LinkedHashMap<>();

        try {
            if (isForceCreateNeeded) {
                Files.deleteIfExists(path);
            }

            if (Files.exists(path)) {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    if (StringUtils.isBlank(line) || line.startsWith("Parent")) continue;

                    String[] parts = line.split(",");
                    if (parts.length > 1) {
                        // Очищаем PieAsset от кавычек для корректного сравнения ключей
                        String pieAssetKey = parts[1].replace("\"", "").trim();
                        dataMap.put(pieAssetKey, line);
                    }
                }
            }

            for (String tokenName : inputTokenNames) {
                String companyName = parseCompanyName(tokenName);
                if (StringUtils.isEmpty(companyName)) continue;

                // Уровень: Company
                // Если компания уже была (например, в корне), она перезапишется новой строкой с родителем Finstore
                String companyRow = String.format(ROW_TEMPLATE, "Finstore", companyName, false);
                dataMap.put(companyName, companyRow);

                // Уровень: Token
                String tokenNameConverted = String.format("%s.%s.CUSTOM_HOLDING",
                        tokenName, parseCurrency(tokenName));
                String tokenRow = String.format(ROW_TEMPLATE, "Finstore//" + companyName, tokenNameConverted, true);

                // Перезаписываем токен, если он уже существовал (убираем дубликат)
                dataMap.put(tokenNameConverted, tokenRow);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                writer.write(HEADER);
                writer.newLine();

                // Проверяем наличие корня Finstore (без кавычек)
                if (!dataMap.containsKey("Finstore")) {
                    writer.write(String.format(ROW_TEMPLATE, "", "Finstore", false));
                    writer.newLine();
                }

                for (String row : dataMap.values()) {
                    writer.write(row);
                    writer.newLine();
                }
            }

            System.out.println("Данные успешно синхронизированы в " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    private String parseCompanyName(String token) {
        int underscoreIndex = token.indexOf('_');
        return (underscoreIndex != -1) ? token.substring(0, underscoreIndex) : token;
    }

    public static String parseCurrency(String input) {
        Pattern pattern = Pattern.compile("\\(([A-Z]{3})_");
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : "Unknown";
    }

    public static FinstoreCategoriesCsvWriter getInstance() {
        return INSTANCE;
    }
}
