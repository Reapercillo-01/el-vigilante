# El Vigilante  ·  alpha 0.1 (versión de prueba)
Creado por Reapercillo.

Cuando Minecraft se cierra por un error, abre una ventana en español que explica
qué pasó y cómo solucionarlo. Primera versión: NeoForge para Minecraft 26.3 (Java 25).

## Archivos
- src/vigilante/neoforge/ElVigilanteMod.java: entrada del mod (solo cliente).
- src/vigilante/LanzadorVigilante.java: lanza el vigilante como proceso aparte.
- src/vigilante/Vigilante.java: espera al juego y revisa crash-reports, hs_err y latest.log.
- src/vigilante/AnalizadorCrash.java: reglas (regex -> causa y solución). Aquí agregas errores.
- src/vigilante/VentanaDiagnostico.java: la ventana con estilo Minecraft (fondo de tierra, panel de piedra, botones biselados).
- resources/META-INF/neoforge.mods.toml: metadatos del mod.

## Cómo compilarlo (5 pasos)
1. Instala Java 25 (por ejemplo Temurin 25) si no lo tienes.
2. Descarga el MDK de NeoForge para Minecraft 26.3 (neoforged.net) y descomprímelo.
3. Descomprime este zip DENTRO de esa carpeta (debe quedar la carpeta "vigilante" junto a gradlew).
4. Abre una terminal en la carpeta del MDK y ejecuta:  vigilante\compilar.bat   (Linux/Mac: sh vigilante/compilar.sh)
5. El .jar queda en build\libs. Cópialo a la carpeta mods y listo.

## Cómo probarlo
Pon el .jar en la carpeta mods de una instancia NeoForge 26.3 con Java 25.
Para provocar un crash de prueba, instala a propósito un mod hecho para otra versión.

## Limitaciones de la alpha
- Sin compilar aún en el entorno donde se escribió (no había javac).
- No avisa si el crash ocurre antes de que cargue el mod.
- Las reglas del analizador son pocas; se amplían con cada error nuevo que encontremos.
