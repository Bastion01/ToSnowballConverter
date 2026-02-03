package com.converter.writers;

import org.apache.commons.lang3.StringUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnowballCategoriesCsvWriter {
    public static SnowballCategoriesCsvWriter instance = new SnowballCategoriesCsvWriter();

    private static final String FILE_NAME = "SnowballCategories.csv";
    private static final String HEADER = "Parent,PieAsset,IsAsset,IsUnallocated,IsLocked,Allocation";
    private static final String ROW_TEMPLATE = "%s,%s,%b,false,false,\"0,0000\"";

    private SnowballCategoriesCsvWriter() {}

    public void writeCategories(Set<String> inputTokenNames, boolean isNewFileCreationNeeded) {
        Path path = Paths.get(FILE_NAME);
        boolean fileExists = Files.exists(path) && path.toFile().length() > 0;
        Set<String> processedCompanies = new HashSet<>();

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            if (!fileExists) {
                writer.write(HEADER);
                writer.newLine();
                writer.write(String.format(ROW_TEMPLATE, "", "Finstore", false));
                writer.newLine();
            }

            for (String tokenName : inputTokenNames) {
                String companyName = parseCompanyName(tokenName);
                if (StringUtils.isEmpty(companyName)) continue;

                // Уровень: Company (под Finstore)
                if (processedCompanies.add(companyName)) {
                    writer.write(String.format(ROW_TEMPLATE, "Finstore", companyName, false));
                    writer.newLine();
                }

                // Уровень: Token (под Company)
                String tokenNameConverted = String.format("%s.%s.CUSTOM_HOLDING",
                        tokenName, parseCurrency(tokenName));

                writer.write(String.format(ROW_TEMPLATE, "Finstore//" + companyName, tokenNameConverted, true));
                writer.newLine();
            }

            System.out.println("Данные успешно добавлены в " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Ошибка при записи файла: " + e.getMessage());
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

    public static SnowballCategoriesCsvWriter getInstance() {
        return instance;
    }
}