@echo off
set APP_NAME=FinstoreToSnowballConverter
set JAR_NAME=FinstoreToSnowballConverter-1.0-SNAPSHOT.jar

echo [0/2] Preparing clean input folder...
:: Clear old data
if exist "target\deploy" rd /s /q "target\deploy"
if exist "target\jpackage-temp" rd /s /q "target\jpackage-temp"

echo [1/2] Building JAR-file...
call mvn clean package -DskipTests

:: Empty folder creation + .jar copying
mkdir "target\deploy"
copy "target\%JAR_NAME%" "target\deploy\"

echo [2/2] Building APP-IMAGE using jpackage...
jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --input target/deploy ^
  --main-jar %JAR_NAME% ^
  --main-class com.converter.FinstoreConverterApp ^
  --icon src/main/resources/icons/app_icon.ico ^
  --vendor "Bastion01" ^
  --dest dist/ ^
  --temp target/jpackage-temp ^
  --verbose

echo Successfully finished!
pause
