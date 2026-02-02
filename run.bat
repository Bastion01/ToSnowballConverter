@echo off
title Finstore to Excel Parser
echo Starting Java Application...

:: Проверка наличия java в системе
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH.
    pause
    exit /b
)

:: Запуск вашего JAR-файла
:: Замените 'web-to-excel-parser-1.0-SNAPSHOT.jar' на реальное имя вашего файла
java -jar "FinstoreToSnowballConverter-1.0-SNAPSHOT.jar"

echo.
echo Operation finished.
pause