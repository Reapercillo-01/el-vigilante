package vigilante;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Analiza el texto de un crash report / log y devuelve diagnósticos en español. */
public final class AnalizadorCrash {

    public record Diagnostico(String titulo, String queParo, String solucion) {}

    public record Resultado(String loader, List<String> sospechosos, List<Diagnostico> diagnosticos,
                            Map<String, String> datos) {}

    private record Regla(Pattern patron, String titulo, String queParo, String solucion) {}

    private static Regla r(String regex, String titulo, String que, String solucion) {
        return new Regla(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), titulo, que, solucion);
    }

    /** Usa {1} en los textos para insertar el primer grupo capturado. */
    private static final List<Regla> REGLAS = List.of(
        r("OutOfMemoryError|GC overhead limit exceeded",
          "Se quedó sin memoria RAM",
          "Minecraft usó toda la memoria que tiene asignada.",
          "Asigna más RAM al juego (por ejemplo 4 GB o 6 GB) en las opciones de tu launcher "
              + "(argumento -Xmx4G). Si ya usas mucha, quita mods pesados o baja la distancia de renderizado."),

        r("EXCEPTION_ACCESS_VIOLATION|SIGSEGV|# A fatal error has been detected by the Java Runtime",
          "Falla grave de Java o del equipo",
          "La máquina virtual de Java se cerró por un error de bajo nivel. Suele ser un problema de "
              + "drivers gráficos, un mod que usa código nativo o, más raramente, hardware inestable.",
          "Actualiza los drivers de tu tarjeta de video, prueba sin mods de rendimiento/shaders y "
              + "revisa que el PC no esté sobrecalentándose."),

        r("UnsupportedClassVersionError",
          "Versión de Java incorrecta",
          "El juego o un mod necesita una versión de Java más nueva que la que estás usando.",
          "Instala la versión de Java que pide tu versión de Minecraft (Java 25 para Minecraft 26.x, "
              + "Java 21 para 1.20.5 a 1.21.x, Java 17 para 1.18 a 1.20.4) y selecciónala en tu launcher."),

        r("Missing or unsupported mandatory dependencies|Mod (\\S+) requires|requires (?:any version of )?\\S+.* which is missing|Unmet dependency",
          "Falta un mod o la versión no coincide",
          "Un mod necesita otro mod (o una versión distinta de Minecraft/loader) que no tienes instalado.",
          "Lee el mensaje de dependencia en el reporte: descarga el mod que falta o la versión "
              + "correcta del mod que lo pide."),

        r("Incompatible mod set|requires minecraft (\\S+)|depends on minecraft|for Minecraft (\\d+\\.\\d+(?:\\.\\d+)?)",
          "Mod hecho para otra versión de Minecraft",
          "Hay un mod que no es compatible con la versión de Minecraft que usas.",
          "Descarga la versión de ese mod que corresponda a tu versión de Minecraft, "
              + "o quítalo de la carpeta mods."),

        r("NoSuchMethodError|NoSuchFieldError|NoClassDefFoundError|ClassNotFoundException|AbstractMethodError",
          "Mod incompatible con la versión o con otro mod",
          "Un mod busca una parte del juego (o de otro mod) que no existe en tu versión. "
              + "Casi siempre es un mod de otra versión de Minecraft o del loader.",
          "Revisa los mods sospechosos de abajo y actualízalos a la versión exacta de tu Minecraft. "
              + "Si no sabes cuál es, quita mods por mitades hasta que deje de fallar."),

        r("Mixin apply failed|MixinApplyError|InvalidMixinException|Mixin transformation of (\\S+) failed",
          "Conflicto entre mods (Mixin)",
          "Dos mods intentan modificar la misma parte del juego, o uno no es compatible con tu versión.",
          "Actualiza los mods mencionados o desinstala uno de los dos que chocan."),

        r("Duplicate mods? found|Found duplicate mod|duplicate mod",
          "Mod duplicado",
          "Tienes el mismo mod dos veces en la carpeta mods.",
          "Borra la versión repetida y deja solo una."),

        r("Exit Code: -?1\\b.*GLFW|GLFW error|OpenGL|Pixel format not accelerated|GL_INVALID",
          "Problema con la tarjeta gráfica",
          "Minecraft no pudo usar la tarjeta de video correctamente.",
          "Actualiza los drivers gráficos, confirma que Minecraft usa la GPU dedicada y "
              + "desactiva shaders o mods de renderizado para probar."),

        r("Failed to load (?:config|configuration)|JsonSyntaxException|ParsingException",
          "Archivo de configuración dañado",
          "Un archivo de configuración tiene errores y el juego no pudo leerlo.",
          "Borra el archivo de configuración del mod mencionado (carpeta config); se vuelve a crear solo."),

        r("java\\.io\\.IOException: There is not enough space|No space left on device",
          "Disco lleno",
          "No hay espacio libre para que el juego guarde datos.",
          "Libera espacio en el disco donde está instalado Minecraft.")
    );

    public static Resultado analizar(String texto) {
        List<Diagnostico> diags = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        for (Regla regla : REGLAS) {
            Matcher m = regla.patron().matcher(texto);
            if (m.find() && vistos.add(regla.titulo())) {
                String dato = "";
                for (int i = 1; i <= m.groupCount(); i++) {
                    if (m.group(i) != null) { dato = m.group(i); break; }
                }
                diags.add(new Diagnostico(
                    regla.titulo(),
                    regla.queParo().replace("{1}", dato),
                    regla.solucion().replace("{1}", dato)));
            }
        }
        return new Resultado(detectarLoader(texto), buscarSospechosos(texto), diags, extraerDatos(texto));
    }

    /** Datos de la partida que aparecen en los crash reports de Minecraft. */
    static Map<String, String> extraerDatos(String t) {
        Map<String, String> m = new LinkedHashMap<>();
        String[][] claves = {
            {"Minecraft Version", "Versión de Minecraft"},
            {"Java Version", "Java"},
            {"Operating System", "Sistema operativo"},
            {"Memory", "Memoria"},
            {"Time", "Hora del crash"}};
        for (String[] c : claves) {
            Matcher x = Pattern.compile("^\\s*" + Pattern.quote(c[0]) + ":\\s*(.+)$", Pattern.MULTILINE).matcher(t);
            if (x.find()) m.put(c[1], x.group(1).trim());
        }
        return m;
    }

    static String detectarLoader(String t) {
        String s = t.toLowerCase();
        if (s.contains("neoforge")) return "NeoForge";
        if (s.contains("quilt")) return "Quilt";
        if (s.contains("fabric")) return "Fabric";
        if (s.contains("minecraftforge") || s.contains("forge mod loader") || s.contains("fml")) return "Forge";
        return "Vanilla / desconocido";
    }

    /** Forge lista "Suspected Mods:"; en Fabric buscamos líneas "Mod ... " en el stack. */
    static List<String> buscarSospechosos(String t) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = Pattern.compile("Suspected Mods?:\\s*\\R((?:\\s+.+\\R?)+)", Pattern.CASE_INSENSITIVE).matcher(t);
        if (m.find()) {
            for (String linea : m.group(1).split("\\R")) {
                String l = linea.trim();
                if (!l.isEmpty() && !l.equalsIgnoreCase("None")) out.add(l);
            }
        }
        Matcher f = Pattern.compile("Mod '([^']+)'|mod ([a-z0-9_\\-]+) \\(", Pattern.CASE_INSENSITIVE).matcher(t);
        while (f.find() && out.size() < 8) {
            String n = f.group(1) != null ? f.group(1) : f.group(2);
            if (n != null) out.add(n);
        }
        return new ArrayList<>(out);
    }

    private AnalizadorCrash() {}
}
