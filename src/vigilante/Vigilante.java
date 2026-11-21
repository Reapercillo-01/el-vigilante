package vigilante;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Proceso independiente: espera a que Minecraft termine y, si fue un crash,
 * muestra una ventana en español con la causa y la solución.
 * Uso: java -cp mod.jar vigilante.Vigilante <pid> <carpetaDelJuego>
 */
public final class Vigilante {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) return;
        long pid = Long.parseLong(args[0]);
        Path juego = Path.of(args[1]);

        Optional<ProcessHandle> proceso = ProcessHandle.of(pid);
        Instant inicio = proceso.flatMap(h -> h.info().startInstant()).orElse(Instant.now());
        proceso.ifPresent(h -> h.onExit().join());
        Thread.sleep(1500); // dar tiempo a que se terminen de escribir los archivos

        List<Path> reportes = nuevos(juego.resolve("crash-reports"), "crash-.*\\.txt", inicio);
        List<Path> fallosJvm = new ArrayList<>(nuevos(juego, "hs_err_pid.*\\.log", inicio));
        Path log = juego.resolve("logs").resolve("latest.log");

        String textoLog = leer(log);
        boolean logActualizado = Files.exists(log) && modificadoDespuesDe(log, inicio);
        boolean cierreNormal = logActualizado && textoLog.contains("Stopping!");

        if (reportes.isEmpty() && fallosJvm.isEmpty() && cierreNormal) return; // todo bien, no molestar

        StringBuilder todo = new StringBuilder();
        for (Path p : reportes) todo.append(leer(p)).append('\n');
        for (Path p : fallosJvm) todo.append(leer(p)).append('\n');
        if (logActualizado) todo.append(ultimasLineas(textoLog, 400));

        AnalizadorCrash.Resultado res = AnalizadorCrash.analizar(todo.toString());
        List<Path> adjuntos = new ArrayList<>(reportes);
        adjuntos.addAll(fallosJvm);
        if (logActualizado) adjuntos.add(log);

        VentanaDiagnostico.mostrar(res, juego, adjuntos, !reportes.isEmpty() || !fallosJvm.isEmpty());
    }

    private static List<Path> nuevos(Path carpeta, String regex, Instant desde) {
        if (!Files.isDirectory(carpeta)) return List.of();
        try (Stream<Path> s = Files.list(carpeta)) {
            return s.filter(p -> p.getFileName().toString().matches(regex))
                    .filter(p -> modificadoDespuesDe(p, desde))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static boolean modificadoDespuesDe(Path p, Instant t) {
        try {
            return Files.getLastModifiedTime(p).toInstant().isAfter(t);
        } catch (IOException e) {
            return false;
        }
    }

    private static String leer(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            try {
                return new String(Files.readAllBytes(p), StandardCharsets.ISO_8859_1);
            } catch (IOException ex) {
                return "";
            }
        }
    }

    private static String ultimasLineas(String texto, int n) {
        String[] l = texto.split("\\R");
        int desde = Math.max(0, l.length - n);
        StringBuilder sb = new StringBuilder();
        for (int i = desde; i < l.length; i++) sb.append(l[i]).append('\n');
        return sb.toString();
    }

    private Vigilante() {}
}
