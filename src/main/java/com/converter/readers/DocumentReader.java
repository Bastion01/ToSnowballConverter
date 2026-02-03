package com.converter.readers;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DocumentReader {
    private static final DocumentReader INSTANCE = new DocumentReader();

    private DocumentReader() {}

    public static DocumentReader getInstance() {
        return INSTANCE;
    }

    /**
     * Загружает список локальных .mhtml файлов, исполняет JS и возвращает список DOM-объектов
     * @param fileNames массив имен файлов
     * @param waitSelector селектор ожидания для каждого файла
     */
    public List<Document> readDocumentsFromMhtml(Set<String> fileNames, String waitSelector) {
        List<Document> documents = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            // Запускаем браузер один раз для всех файлов ради производительности
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            for (String fileName : fileNames) {
                Path filePath = Paths.get(fileName);

                // Проверка существования файла
                if (!Files.exists(filePath)) {
                    System.err.println("Файл не найден и будет пропущен: " + fileName);
                    continue;
                }

                try {
                    String absolutePath = filePath.toUri().toString();
                    page.navigate(absolutePath);

                    // Ждем отрисовки контента
                    page.waitForSelector(waitSelector);

                    String content = page.content();
                    documents.add(Jsoup.parse(content));

                    System.out.println("Файл успешно прочитан: " + fileName);
                } catch (Exception e) {
                    System.err.println("Ошибка при обработке файла " + fileName + ": " + e.getMessage());
                }
            }

            browser.close();
        } catch (Exception e) {
            throw new RuntimeException("Критическая ошибка Playwright: " + e.getMessage(), e);
        }

        return documents;
    }
}
