package com.converter;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class Launcher {
    private static FileLock lock;
    private static FileChannel channel;

    public static void main(String[] args) {
        FinstoreConverterApp.main(args);
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

