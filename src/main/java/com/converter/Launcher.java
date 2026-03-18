package com.converter;

import io.sentry.Sentry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Properties;

public class Launcher {
    private static FileLock lock;
    private static FileChannel channel;

    public static void main(String[] args) {
        String version = getProperty("app.version");
        Sentry.init(options -> {
            options.setDsn(getProperty("sentry.dsn"));
            options.setRelease("finstore-converter@" + version);
            Sentry.setTag("app_version", version);
            options.setTracesSampleRate(1.0);
        });

        FinstoreConverterApp.main(args);
    }

    private static String getProperty(String key) {
        try (InputStream input = Launcher.class.getClassLoader().getResourceAsStream("version.properties")) {
            Properties prop = new Properties();
            if (input == null) return "unknown";
            prop.load(input);
            return prop.getProperty(key, "unknown");
        } catch (IOException ex) {
            return "unknown";
        }
    }

    public static boolean isAlreadyRunning() {
        try {
            // Создаем файл-индикатор во временной папке
            File file = new File(System.getProperty("java.io.tmpdir"), "finstore_converter.lock");
            channel = new RandomAccessFile(file, "rw").getChannel();

            // Пытаемся заблокировать файл
            lock = channel.tryLock();

            if (lock == null) {
                return true; // Файл уже заблокирован другим процессом
            }

            // Добавляем завершение при выходе (не обязательно, но полезно)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (lock != null) lock.release();
                    if (channel != null) channel.close();
                } catch (Exception e) { e.printStackTrace(); }
            }));

            return false;
        } catch (Exception e) {
            return false; // Если произошла ошибка (например, нет прав), разрешаем запуск
        }
    }
}

