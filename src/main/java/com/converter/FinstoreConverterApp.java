package com.converter;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.Styles;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.read.CyclicBufferAppender;
import com.converter.model.TableRow;
import com.converter.model.TheadRow;
import com.converter.services.FinstoreToSnowballConversionService;
import com.converter.services.ResultKey;
import io.sentry.Attachment;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jsoup.nodes.Document;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FinstoreConverterApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(FinstoreConverterApp.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // Списки для хранения выбранных файлов
    private final List<File> mhtmlFiles = new ArrayList<>();
    private File categoriesCsvFile = null;

    // Элементы UI для обновления состояния
    private Label mhtmlFileListLabel;
    private Label csvFileNameLabel;
    private VBox csvUploadArea;
    private CheckBox addCategoriesCb;
    private ProgressBar progressBar;
    private Label progressStatusLabel;
    private final BooleanProperty isProcessing = new SimpleBooleanProperty(false);
    private CheckBox cbFinstoreXlsx, cbSnowballXlsx, cbSnowballCsv;

    @Override
    public void start(Stage stage) {
        // Установка темной темы Atalanta
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // 0. Проверяет нет ли других версий запущенного приложения
        if (Launcher.isAlreadyRunning()) {
            showErrorAndExit();
            return;
        }

        VBox root = new VBox(10);
        root.setPadding(new Insets(10, 10, 10, 10));
        root.setAlignment(Pos.TOP_CENTER);

        // 1. Top Bar
        HBox topBar = createTopBar();

        // 2. Title
        VBox titleArea = new VBox(10);
        titleArea.setAlignment(Pos.TOP_CENTER);
        Label titleLabel = new Label("Finstore to Snowball Converter");
        titleLabel.getStyleClass().addAll(Styles.TITLE_2, Styles.TEXT_BOLD); // Крупный и жирный шрифт
        Separator sepTop = new Separator();
        sepTop.setPadding(Insets.EMPTY);
        Separator sepBottom = new Separator();
        sepBottom.setPadding(Insets.EMPTY);
        titleArea.getChildren().addAll(sepTop, titleLabel, sepBottom);

        // 3. MHTML Upload Area
        VBox uploadInputArea = new VBox(10);
        uploadInputArea.setAlignment(Pos.TOP_CENTER);
        Label inputDataLabel = new Label("Входные данные:");
        inputDataLabel.setPadding(Insets.EMPTY);
        VBox mhtmlUploadArea = createUploadArea(
                "Перетащите .mhtml файлы сюда или нажмите для выбора",
                ".mhtml", true
        );
        mhtmlUploadArea.disableProperty().bind(isProcessing);
        mhtmlFileListLabel = new Label("Файлы не выбраны");
        mhtmlFileListLabel.setWrapText(true);
        mhtmlFileListLabel.setPadding(Insets.EMPTY);
        uploadInputArea.getChildren().addAll(inputDataLabel, mhtmlUploadArea, mhtmlFileListLabel);

        // 4. Categories Section
        VBox categoriesArea = new VBox(10);
        categoriesArea.setAlignment(Pos.TOP_CENTER);
        addCategoriesCb = new CheckBox("Добавить категории к существующим");
        addCategoriesCb.disableProperty().bind(isProcessing);
        csvUploadArea = createUploadArea("Перетащите .csv файл сюда или нажмите для выбора", ".csv", false);
        csvUploadArea.disableProperty().bind(isProcessing.or(addCategoriesCb.selectedProperty().not()));
        csvFileNameLabel = new Label("Файл не выбран");
        csvFileNameLabel.setPadding(Insets.EMPTY);

        addCategoriesCb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) resetCsvSelection();
        });
        Separator outputSeparator = new Separator();
        outputSeparator.setPadding(Insets.EMPTY);
        categoriesArea.getChildren().addAll(addCategoriesCb, csvUploadArea, csvFileNameLabel, outputSeparator);

        // 5. Output Selection
        VBox outputArea = new VBox(10);
        outputArea.setAlignment(Pos.TOP_CENTER);
        HBox outputOptions = new HBox(10);
        outputOptions.setAlignment(Pos.CENTER);
        cbFinstoreXlsx = new CheckBox("FinstoreReport XLSX");
        cbSnowballXlsx = new CheckBox("SnowballReport XLSX");
        cbSnowballCsv = new CheckBox("SnowballCategories CSV");
        cbFinstoreXlsx.setSelected(true);
        cbSnowballXlsx.setSelected(true);
        cbSnowballCsv.setSelected(true);
        cbFinstoreXlsx.disableProperty().bind(isProcessing);
        cbSnowballXlsx.disableProperty().bind(isProcessing);
        cbSnowballCsv.disableProperty().bind(isProcessing);
        outputOptions.getChildren().addAll(cbFinstoreXlsx, cbSnowballXlsx, cbSnowballCsv);
        Label outputDataLabel = new Label("Выходные данные:");
        outputDataLabel.setPadding(Insets.EMPTY);
        outputArea.getChildren().addAll(outputDataLabel, outputOptions);

        // 6. Progress Area
        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.TOP_CENTER);
        progressStatusLabel = new Label("Ожидание действий...");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(600);
        progressBox.getStyleClass().add(Styles.LARGE);
        progressBox.getChildren().addAll(progressStatusLabel, progressBar);

        // 7. Main Action Button
        Button convertBtn = new Button("КОНВЕРТИРОВАТЬ И СОХРАНИТЬ .ZIP");
        convertBtn.setPadding(Insets.EMPTY);
        convertBtn.getStyleClass().addAll("accent", Styles.SUCCESS);
        convertBtn.setPrefSize(600, 40);
        convertBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        convertBtn.disableProperty().bind(
                cbFinstoreXlsx.selectedProperty().not()
                .and(cbSnowballXlsx.selectedProperty().not())
                .and(cbSnowballCsv.selectedProperty().not())
                .or(isProcessing)
        );
        convertBtn.setOnAction(e -> startConversionProcess(convertBtn));

        // Assembly
        root.getChildren().addAll(
                topBar,
                titleArea,
                uploadInputArea,
                categoriesArea,
                outputArea,
                progressBox,
                convertBtn
        );

        Scene scene = new Scene(root, 600, 650);
        stage.setTitle("Finstore to Snowball Converter");
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app_icon.png")));
        stage.show();
        root.requestFocus();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(5);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(Insets.EMPTY);

        Button helpBtn = new Button("Помощь");
        helpBtn.setOnAction(e -> getHostServices().showDocument("https://google.com"));

        Button aboutBtn = new Button("Об авторе");
        aboutBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Об авторе");
            alert.setHeaderText(null);
            alert.setContentText("Made by Bastion01\nGitHub: https://github.com/Bastion01/");
            alert.showAndWait();
        });

        helpBtn.getStyleClass().add(Styles.SMALL);
        aboutBtn.getStyleClass().add(Styles.SMALL);
        topBar.getChildren().addAll(helpBtn, aboutBtn);
        return topBar;
    }

    private VBox createUploadArea(String text, String extension, boolean multiple) {
        VBox area = new VBox(10);
        area.setAlignment(Pos.CENTER);
        area.setPadding(Insets.EMPTY);
        area.setPrefHeight(100);
        area.setStyle("-fx-border-color: #444; -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 5;");

        Label label = new Label(text);
        Button btn = new Button("Выбрать файл" + (multiple ? "ы" : ""));

        area.getChildren().addAll(label, btn);

        // Click Logic
        btn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Files", "*" + extension));
            if (multiple) {
                List<File> selected = fileChooser.showOpenMultipleDialog(null);
                if (selected != null) handleFilesInput(selected, true);
            } else {
                File selected = fileChooser.showOpenDialog(null);
                if (selected != null) handleFilesInput(List.of(selected), false);
            }
        });

        // Drag and Drop Logic
        area.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        area.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                handleFilesInput(db.getFiles(), multiple);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        return area;
    }

    private void handleFilesInput(List<File> files, boolean isMhtml) {
        List<String> oversizedFiles = new ArrayList<>();

        if (isMhtml) {
            mhtmlFiles.clear();
            files.stream()
                    .filter(f -> f.getName().toLowerCase().endsWith(".mhtml"))
                    .limit(10)
                    .forEach(f -> {
                        if (f.length() <= MAX_FILE_SIZE) {
                            mhtmlFiles.add(f);
                        } else {
                            oversizedFiles.add(f.getName());
                        }
                    });

            if (mhtmlFiles.isEmpty()) {
                mhtmlFileListLabel.setText("Файлы не выбраны");
            } else {
                StringBuilder sb = new StringBuilder("Выбрано: ");
                for (int i = 0; i < mhtmlFiles.size(); i++) {
                    sb.append(mhtmlFiles.get(i).getName());
                    if (i < mhtmlFiles.size() - 1) sb.append(", ");
                }
                mhtmlFileListLabel.setText(sb.toString());
            }
        } else {
            File f = files.get(0);
            if (f.getName().toLowerCase().endsWith(".csv")) {
                if (f.length() <= MAX_FILE_SIZE) {
                    categoriesCsvFile = f;
                    csvFileNameLabel.setText("Выбран: " + f.getName());
                } else {
                    oversizedFiles.add(f.getName());
                    resetCsvSelection();
                }
            }
        }

        // Если были слишком большие файлы — показываем один алерт на всё
        if (!oversizedFiles.isEmpty()) {
            showError("Следующие файлы превышают лимит 10 МБ и не были добавлены:\n"
                    + String.join("\n", oversizedFiles));
        }
    }

    private void startConversionProcess(Button btn) {
        if (mhtmlFiles.isEmpty()) {
            showError("Пожалуйста, выберите входные MHTML файлы.");
            return;
        }

        // 1. Выбор директории для сохранения
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите папку для сохранения ZIP архива");

        // Устанавливаем текущую рабочую директорию как начальную
        String downloadsPath = System.getProperty("user.home") + "/Downloads";
        File defaultDirectory = new File(downloadsPath);
        if (defaultDirectory.exists()) {
            directoryChooser.setInitialDirectory(defaultDirectory);
        }

        File selectedDirectory = directoryChooser.showDialog(btn.getScene().getWindow());

        // Если пользователь закрыл окно выбора — ничего не делаем
        if (selectedDirectory == null) {
            return;
        }

        // 3. Запуск основной логики
        isProcessing.set(true);

        new Thread(() -> {
            try {
                FinstoreToSnowballConversionService toSnowballConversionService = FinstoreToSnowballConversionService.getInstance();
                updateUI("Инициализация", 0.1);
                List<Document> documents = toSnowballConversionService.readDocuments(Set.copyOf(mhtmlFiles));
                Thread.sleep(1000);

                updateUI("Разбор входных данных", 0.4);
                TheadRow headers = toSnowballConversionService.readHeaders(documents);
                List<TableRow> data = toSnowballConversionService.readData(documents);
                Thread.sleep(1000);

                updateUI("Генерация отчётов", 0.7);
                List<File> reports = toSnowballConversionService.generateReports(
                        categoriesCsvFile, headers, data, collectResultKeys()
                );

                updateUI("Архивация", 0.9);
                File finalZipFile = processAndSaveZip(selectedDirectory, reports);
                Thread.sleep(1000);

                updateUI("Успех", 1.0);
                Platform.runLater(() -> {
                    isProcessing.set(false);
                    showSuccessMessage("Архив успешно сохранен:\n" + finalZipFile.getAbsolutePath());
                    resetToInitialState();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    isProcessing.set(false);
                    handleGlobalError(e);
                });
            }
        }).start();
    }

    private File processAndSaveZip(File selectedDirectory, List<File> reports) throws IOException {
        // 1. Формируем имя файла архива
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH.mm.ss"));
        String zipFileName = "FinstoreToSnowballConverted_" + timestamp + ".zip";
        File zipFile = new File(selectedDirectory, zipFileName);

        // 2. Создаем ZIP архив
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (File reportFile : reports) {
                // Безопасная проверка: не null и файл существует физически
                if (reportFile != null && reportFile.exists()) {

                    try (FileInputStream fis = new FileInputStream(reportFile)) {
                        // Создаем запись внутри архива (имя файла берется из оригинального файла)
                        ZipEntry zipEntry = new ZipEntry(reportFile.getName());
                        zos.putNextEntry(zipEntry);

                        byte[] buffer = new byte[8192]; // Буфер 8КБ для оптимизации
                        int length;
                        while ((length = fis.read(buffer)) >= 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                    }
                }

                try {
                    Files.delete(reportFile.toPath());
                } catch (IOException e) {
                    System.err.println("Не удалось удалить временный файл: " + reportFile.getName());
                }
            }
        }
        return zipFile;
    }

    private List<ResultKey> collectResultKeys() {
        List<ResultKey> resultKeys = new ArrayList<>();
        if (cbFinstoreXlsx.isSelected()) {
            resultKeys.add(ResultKey.FINSTORE_REPORT);
        }
        if (cbSnowballXlsx.isSelected()) {
            resultKeys.add(ResultKey.SNOWBALL_REPORT);
        }
        if (cbSnowballCsv.isSelected()) {
            resultKeys.add(ResultKey.SNOWBALL_CATEGORIES);
        }
        return resultKeys;
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Завершено");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void updateUI(String status, double progress) {
        Platform.runLater(() -> {
            progressStatusLabel.setText(status);
            progressBar.setProgress(progress);
        });
    }

    private void resetToInitialState() {
        mhtmlFiles.clear();
        categoriesCsvFile = null;
        mhtmlFileListLabel.setText("Файлы не выбраны");
        csvFileNameLabel.setText("Файл не выбран");
        addCategoriesCb.setSelected(false);
        cbFinstoreXlsx.setSelected(true);
        cbSnowballXlsx.setSelected(true);
        cbSnowballCsv.setSelected(true);
        progressBar.setProgress(0);
        progressStatusLabel.setText("Готово! Файлы сохранены.");
    }

    private void handleGlobalError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Что-то пошло не так...");
        alert.setContentText("Отправить отчёт об ошибке?");

        ButtonType btnYes = new ButtonType("Да");
        ButtonType btnNo = new ButtonType("Нет");
        alert.getButtonTypes().setAll(btnYes, btnNo);

        alert.showAndWait().ifPresent(type -> {
            if (type == btnYes) {
                saveErrorReport(e);
            }
            Platform.exit();
            System.exit(0);
        });
    }

    private void saveErrorReport(Exception e) {
        String appData = System.getenv("APPDATA");
        File logDir = new File(appData, "FinstoreConverter/logs");
        if (!logDir.exists()) logDir.mkdirs();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss"));
        File logFile = new File(logDir, "ERROR_REPORT_" + timestamp + ".log");

        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile))) {
            pw.println("=== APPLICATION LOGS (Last 256 events) ===");

            // Магия: достаем логи из памяти Logback
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            CyclicBufferAppender<ILoggingEvent> memoryAppender =
                    (CyclicBufferAppender<ILoggingEvent>) context.getLogger("ROOT").getAppender("MEMORY");

            if (memoryAppender != null) {
                int count = memoryAppender.getLength();
                for (int i = 0; i < count; i++) {
                    ILoggingEvent event = memoryAppender.get(i);
                    // Форматируем строку лога вручную или используем паттерн
                    pw.printf("%s [%s] %-5s %s - %s%n",
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault()),
                            event.getThreadName(),
                            event.getLevel(),
                            event.getLoggerName(),
                            event.getFormattedMessage());
                }
            }

            pw.println("\n=== EXCEPTION STACKTRACE ===");
            e.printStackTrace(pw);

            pw.println("\n--- System Info ---");
            pw.println("OS: " + System.getProperty("os.name"));
            pw.println("Java: " + System.getProperty("java.version"));

            sendEventToSentry(e, logFile);
        } catch (Exception ioException) {
            ioException.printStackTrace();
        }
    }

    private void sendEventToSentry(Exception e, File logFile) {
        Sentry.withScope(scope -> {
            // Создаем объект Attachment из пути к файлу
            Attachment attachment = new Attachment(logFile.getAbsolutePath());
            scope.addAttachment(attachment);

            // Создаем событие
            SentryEvent event = new SentryEvent(e);
            event.setLevel(SentryLevel.ERROR);

            Message message = new Message();
            message.setMessage("User reported error: " + e.getMessage());
            event.setMessage(message);

            Sentry.captureEvent(event);
        });
    }


    private void resetCsvSelection() {
        categoriesCsvFile = null;
        csvFileNameLabel.setText("Файл не выбран");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg);
        a.show();
    }

    private void showErrorAndExit() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Ошибка запуска");
        alert.setHeaderText("Приложение уже запущено");
        alert.setContentText("Пожалуйста, закройте предыдущую копию программы.");

        // Alert подхватит стиль PrimerDark автоматически
        alert.showAndWait();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

