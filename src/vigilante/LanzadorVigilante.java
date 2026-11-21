package vigilante;

import java.io.File;
import java.nio.file.Path;

/**
 * Esto es lo único que tu mod debe llamar al iniciar (cualquier loader):
 *     LanzadorVigilante.iniciar(carpetaDelJuego);
 * Lanza el Vigilante como proceso aparte, que sobrevive al cierre de Minecraft.
 */
public final class LanzadorVigilante {

    public static final String VERSION = "0.1.0-alpha";

    private static boolean yaIniciado = false;

    public static synchronized void iniciar(Path carpetaDelJuego) {
        if (yaIniciado) return;
        yaIniciado = true;
        try {
            File jar = ubicarJar();
            String java = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").toLowerCase().contains("win") ? "javaw.exe" : "java").toString();

            ProcessBuilder pb = new ProcessBuilder(
                    java, "-Xmx64m", "-cp", jar.getAbsolutePath(), "vigilante.Vigilante",
                    Long.toString(ProcessHandle.current().pid()),
                    carpetaDelJuego.toAbsolutePath().toString());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start();
        } catch (Exception e) {
            // Nunca romper el juego por culpa del vigilante.
            System.err.println("[Vigilante] No se pudo iniciar: " + e);
        }
    }

    /** Ubica el .jar del mod; en NeoForge la ruta puede venir como jar:file:...!/ */
    private static File ubicarJar() throws Exception {
        String loc = LanzadorVigilante.class.getProtectionDomain().getCodeSource().getLocation().toString();
        if (loc.startsWith("jar:")) loc = loc.substring(4);
        int fin = loc.indexOf("!/");
        if (fin >= 0) loc = loc.substring(0, fin);
        return new File(new java.net.URI(loc));
    }

    private LanzadorVigilante() {}
}
