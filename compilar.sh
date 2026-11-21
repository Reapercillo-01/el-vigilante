#!/bin/sh
# El Vigilante - compilar (Linux/Mac). Ejecutar desde la raiz del MDK: sh vigilante/compilar.sh
[ -f gradlew ] || { echo "Ejecuta esto desde la carpeta raiz del MDK de NeoForge."; exit 1; }
[ -d vigilante/src/vigilante ] || { echo "Falta la carpeta vigilante dentro del MDK."; exit 1; }
rm -rf src/main/java/com
mkdir -p src/main/java src/main/templates/META-INF
cp -r vigilante/src/vigilante src/main/java/
cp vigilante/resources/META-INF/neoforge.mods.toml src/main/templates/META-INF/neoforge.mods.toml
chmod +x gradlew && ./gradlew build
echo "Si todo salio bien, tu mod esta en build/libs"
