package com.converter.readers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Paths;

public class DocumentReader {
    private static DocumentReader instance = new DocumentReader();

    private DocumentReader() {}

    public static DocumentReader getInstance() {
        return instance;
    }

    /**
     * Загружает локальный .mhtml файл, исполняет JavaScript и возвращает DOM
     * @param fileName имя файла в корне проекта (например, "page.mhtml")
     * @param waitSelector селектор элемента, появления которого нужно дождаться (например, "text=Вид операции")
     */
    public Document readDocumentFromMhtml(String fileName, String waitSelector) {
        try (Playwright playwright = Playwright.create()) {
            // Запускаем Chromium в фоновом режиме
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            // Преобразуем имя файла в абсолютный URI для браузера
            String absolutePath = Paths.get(fileName).toUri().toString();

            // Переходим по пути файла
            page.navigate(absolutePath);

            // Ждем, пока React/MUI отрисует таблицу с данными
            page.waitForSelector(waitSelector);

            // Получаем HTML после выполнения всех скриптов
            String content = page.content();

            Document doc = Jsoup.parse(content);

            browser.close();
            return doc;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении .mhtml файла через Playwright: " + e.getMessage(), e);
        }
    }
}
