@echo off
rem El Vigilante - compilar (Windows)
rem Ejecutar desde la carpeta raiz del MDK de NeoForge (donde esta gradlew.bat):
rem     vigilante\compilar.bat
if not exist gradlew.bat (
  echo No encuentro gradlew.bat. Ejecuta este script desde la carpeta raiz del MDK de NeoForge.
  pause
  exit /b 1
)
if not exist vigilante\src\vigilante (
  echo No encuentro la carpeta "vigilante". Descomprime el zip dentro del MDK.
  pause
  exit /b 1
)
rem Quitar el mod de ejemplo del MDK
if exist src\main\java\com rmdir /s /q src\main\java\com
xcopy /e /i /y vigilante\src\vigilante src\main\java\vigilante >nul
if not exist src\main\templates\META-INF mkdir src\main\templates\META-INF
copy /y vigilante\resources\META-INF\neoforge.mods.toml src\main\templates\META-INF\neoforge.mods.toml >nul
call gradlew.bat build
echo.
echo Si todo salio bien, tu mod esta en la carpeta build\libs (el .jar sin "sources").
echo Copialo a la carpeta mods de Minecraft.
pause
