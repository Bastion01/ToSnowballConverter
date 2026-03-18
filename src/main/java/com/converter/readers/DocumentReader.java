package com.converter.readers;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class DocumentReader {
    private static final Logger logger = LoggerFactory.getLogger(DocumentReader.class);
    private static final DocumentReader INSTANCE = new DocumentReader();

    private DocumentReader() {}

    public static DocumentReader getInstance() {
        return INSTANCE;
    }

    /**
     * Загружает список локальных .mhtml файлов, исполняет JS и возвращает список DOM-объектов
     * @param mhtmlFiles массив файлов
     * @param waitSelector селектор ожидания для каждого файла
     */
    public List<Document> readDocumentsFromMhtml(Set<File> mhtmlFiles, String waitSelector) {
        List<Document> documents = new ArrayList<>();

        try (Playwright playwright = createOrReusePlaywrightEntity()) {
            // Запускаем браузер один раз для всех файлов ради производительности
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            for (File mhtmlFile : mhtmlFiles) {
                String fileName = mhtmlFile.getName();
                Path filePath = Paths.get(mhtmlFile.getPath());

                try {
                    String absolutePath = filePath.toUri().toString();
                    page.navigate(absolutePath);

                    // Ждем отрисовки контента
                    page.waitForSelector(waitSelector);

                    String content = page.content();
                    documents.add(Jsoup.parse(content));

                    logger.debug("File successfully read: {}", fileName);
                } catch (Exception e) {
                    throw new RuntimeException("Error during file read or handling " + fileName + ": " + e.getMessage(), e);
                }
            }

            browser.close();
        } catch (Exception e) {
            throw new RuntimeException("Playwright critical error: " + e.getMessage(), e);
        }

        return documents;
    }

    private static Playwright createOrReusePlaywrightEntity() throws URISyntaxException {
        Path jarPath = Paths.get(DocumentReader.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParent();
        Path internalBrowsers = jarPath.resolve("browsers");

        if (Files.exists(internalBrowsers)) {
            Map<String, String> env = new HashMap<>();
            env.put("PLAYWRIGHT_BROWSERS_PATH", internalBrowsers.toAbsolutePath().toString());
            logger.debug("Full version detected. Using internal browsers.");
            return Playwright.create(new Playwright.CreateOptions().setEnv(env));
        }

        logger.debug("Lite version mode. Using system browsers.");
        return Playwright.create();
    }
}
