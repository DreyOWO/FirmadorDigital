@echo off
REM Migration script to organize source files into core and GUI modules

echo === Firmador Source Migration Script ===
echo This script will organize source files into firmador-core and firmador-gui modules
echo.

REM Create directory structure
echo Creating directory structure...
mkdir firmador-core\src\main\java\cr\libre\firmador 2>nul
mkdir firmador-core\src\main\java9 2>nul
mkdir firmador-core\src\main\resources 2>nul
mkdir firmador-core\src\test\java 2>nul

mkdir firmador-gui\src\main\java\cr\libre\firmador 2>nul
mkdir firmador-gui\src\main\java9 2>nul
mkdir firmador-gui\src\main\resources 2>nul
mkdir firmador-gui\src\test\java 2>nul

REM Copy core packages (no GUI dependencies)
echo Copying core packages...
xcopy /E /I /Y src\main\java\cr\libre\firmador\cards firmador-core\src\main\java\cr\libre\firmador\cards
xcopy /E /I /Y src\main\java\cr\libre\firmador\signers firmador-core\src\main\java\cr\libre\firmador\signers
xcopy /E /I /Y src\main\java\cr\libre\firmador\validators firmador-core\src\main\java\cr\libre\firmador\validators
xcopy /E /I /Y src\main\java\cr\libre\firmador\documents firmador-core\src\main\java\cr\libre\firmador\documents
xcopy /E /I /Y src\main\java\cr\libre\firmador\ooxml firmador-core\src\main\java\cr\libre\firmador\ooxml
xcopy /E /I /Y src\main\java\cr\libre\firmador\remote firmador-core\src\main\java\cr\libre\firmador\remote
xcopy /E /I /Y src\main\java\cr\libre\firmador\connections firmador-core\src\main\java\cr\libre\firmador\connections
xcopy /E /I /Y src\main\java\cr\libre\firmador\services firmador-core\src\main\java\cr\libre\firmador\services

REM Copy core utility classes
echo Copying core utility classes...
copy /Y src\main\java\cr\libre\firmador\CertificateManager.java firmador-core\src\main\java\cr\libre\firmador\
copy /Y src\main\java\cr\libre\firmador\MessageUtils.java firmador-core\src\main\java\cr\libre\firmador\
copy /Y src\main\java\cr\libre\firmador\Report.java firmador-core\src\main\java\cr\libre\firmador\
copy /Y src\main\java\cr\libre\firmador\Settings.java firmador-core\src\main\java\cr\libre\firmador\
copy /Y src\main\java\cr\libre\firmador\SettingsManager.java firmador-core\src\main\java\cr\libre\firmador\
copy /Y src\main\java\cr\libre\firmador\SingleInstanceManager.java firmador-core\src\main\java\cr\libre\firmador\
copy /Y src\main\java\cr\libre\firmador\ConfigListener.java firmador-core\src\main\java\cr\libre\firmador\

REM Copy GUI packages
echo Copying GUI packages...
xcopy /E /I /Y src\main\java\cr\libre\firmador\gui firmador-gui\src\main\java\cr\libre\firmador\gui
xcopy /E /I /Y src\main\java\cr\libre\firmador\plugins firmador-gui\src\main\java\cr\libre\firmador\plugins
xcopy /E /I /Y src\main\java\cr\libre\firmador\previewers firmador-gui\src\main\java\cr\libre\firmador\previewers

REM Copy main class
echo Copying main application class...
copy /Y src\main\java\Firmador.java firmador-gui\src\main\java\

REM Copy Java 9+ specific code
echo Copying Java 9+ code...
if exist src\main\java9 (
    xcopy /E /I /Y src\main\java9\* firmador-core\src\main\java9\ 2>nul
    xcopy /E /I /Y src\main\java9\* firmador-gui\src\main\java9\ 2>nul
)

REM Copy resources
echo Copying resources...
REM Core resources (certificates, messages, etc.)
xcopy /E /I /Y src\main\resources\certs firmador-core\src\main\resources\certs
xcopy /E /I /Y src\main\resources\dgt firmador-core\src\main\resources\dgt
xcopy /E /I /Y src\main\resources\xml firmador-core\src\main\resources\xml
xcopy /E /I /Y src\main\resources\xslt firmador-core\src\main\resources\xslt
copy /Y src\main\resources\dss-messages_es.properties firmador-core\src\main\resources\
copy /Y src\main\resources\messages*.properties firmador-core\src\main\resources\

REM GUI resources (icons, images)
copy /Y src\main\resources\*.png firmador-gui\src\main\resources\ 2>nul
copy /Y src\main\resources\*.ico firmador-gui\src\main\resources\ 2>nul
copy /Y src\main\resources\install_windows.vbs firmador-gui\src\main\resources\ 2>nul
copy /Y src\main\resources\nonPreview.pdf firmador-gui\src\main\resources\ 2>nul
xcopy /E /I /Y src\main\resources\icons firmador-gui\src\main\resources\icons 2>nul

REM Copy tests
echo Copying tests...
REM Core tests
xcopy /E /I /Y src\test\java\cr\libre\firmador\signers firmador-core\src\test\java\cr\libre\firmador\signers 2>nul
xcopy /E /I /Y src\test\java\cr\libre\firmador\validators firmador-core\src\test\java\cr\libre\firmador\validators 2>nul
copy /Y src\test\java\cr\libre\firmador\TestSettingsManager.java firmador-core\src\test\java\cr\libre\firmador\ 2>nul

REM GUI tests
xcopy /E /I /Y src\test\java\cr\libre\firmador\gui firmador-gui\src\test\java\cr\libre\firmador\gui 2>nul
xcopy /E /I /Y src\test\java\cr\libre\firmador\plugins firmador-gui\src\test\java\cr\libre\firmador\plugins 2>nul
xcopy /E /I /Y src\test\java\cr\libre\firmador\previewers firmador-gui\src\test\java\cr\libre\firmador\previewers 2>nul

REM Copy test utilities
xcopy /E /I /Y src\test\java\utils firmador-core\src\test\java\utils 2>nul

echo.
echo === Migration Complete ===
echo.
echo Next steps:
echo 1. Review the migrated files in firmador-core\ and firmador-gui\
echo 2. Build the project: mvn clean install -f pom-parent.xml
echo 3. Test the GUI application: java -jar firmador-gui\target\firmador.jar
echo.
echo Note: The original src\ directory is preserved. You can delete it after verifying the migration.
pause

@REM Made with Bob
