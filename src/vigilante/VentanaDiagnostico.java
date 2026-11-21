package vigilante;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/** Ventana con estilo Minecraft: fondo de tierra, panel de piedra, botones biselados. */
final class VentanaDiagnostico {

    private static final Color AMARILLO = new Color(0xFFFF55);
    private static final Color ROJO = new Color(0xFF5555);
    private static final Color VERDE = new Color(0x55FF55);
    private static final Color ORO = new Color(0xFFAA00);
    private static final Color AQUA = new Color(0x55FFFF);
    private static final Color PIEDRA = new Color(0xC6C6C6);
    private static final Color RANURA = new Color(0x373737);
    private static final Font FUENTE = new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final int ANCHO_TEXTO = 470;

    static void mostrar(AnalizadorCrash.Resultado res, Path juego, List<Path> adjuntos, boolean huboCrash) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("El Vigilante");
            f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            Fondo fondo = new Fondo();
            fondo.setLayout(new BorderLayout());
            fondo.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

            Piedra panel = new Piedra();
            panel.setLayout(new BorderLayout(0, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            panel.add(cabecera(huboCrash), BorderLayout.NORTH);
            panel.add(lista(res), BorderLayout.CENTER);
            panel.add(pie(f, res, juego, adjuntos), BorderLayout.SOUTH);

            fondo.add(panel, BorderLayout.CENTER);
            f.setContentPane(fondo);
            f.setSize(640, 680);
            f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.setAlwaysOnTop(true);
            f.setVisible(true);
        });
    }

    // ---------- secciones ----------

    private static JPanel cabecera(boolean huboCrash) {
        JPanel t = tarjeta(AMARILLO);
        t.add(html("<font color='#FFFF55' size='6'><b>EL VIGILANTE</b></font>"));
        String msg = huboCrash ? "¡Minecraft se cerró por un error!" : "Minecraft se cerró de forma inesperada";
        t.add(html("<font color='#FF5555'><b>" + esc(msg) + "</b></font><br>"
                + "<font color='#AAAAAA'>Esto fue lo que encontré y cómo arreglarlo:</font>"));
        return t;
    }

    private static JScrollPane lista(AnalizadorCrash.Resultado res) {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        if (res.diagnosticos().isEmpty()) {
            JPanel t = tarjeta(ORO);
            t.add(html("<font color='#FFAA00'><b>No pude identificar la causa exacta</b></font><br>"
                    + "Prueba esto: quita los mods que añadiste más recientemente, actualiza los drivers "
                    + "de tu tarjeta gráfica y revisa el reporte con el botón de abajo."));
            col.add(t);
            col.add(espacio());
        }
        boolean primero = true;
        for (AnalizadorCrash.Diagnostico d : res.diagnosticos()) {
            JPanel t = tarjeta(primero ? ROJO : ORO);
            t.add(html("<font color='" + (primero ? "#FF5555" : "#FFAA00") + "'><b>"
                    + (primero ? "CAUSA MÁS PROBABLE: " : "TAMBIÉN: ") + esc(d.titulo()) + "</b></font>"));
            t.add(html("<font color='#55FFFF'><b>QUÉ PASÓ</b></font><br>" + esc(d.queParo())));
            t.add(html("<font color='#55FF55'><b>SOLUCIÓN</b></font><br>" + esc(d.solucion())));
            col.add(t);
            col.add(espacio());
            primero = false;
        }
        if (!res.sospechosos().isEmpty()) {
            JPanel t = tarjeta(ORO);
            StringBuilder sb = new StringBuilder("<font color='#FFAA00'><b>MODS SOSPECHOSOS</b></font>");
            for (String s : res.sospechosos()) sb.append("<br>- ").append(esc(s));
            t.add(html(sb.toString()));
            col.add(t);
            col.add(espacio());
        }
        JPanel datos = tarjeta(AQUA);
        StringBuilder sb = new StringBuilder("<font color='#55FFFF'><b>DATOS DE LA PARTIDA</b></font>");
        sb.append("<br>Loader: ").append(esc(res.loader()));
        for (Map.Entry<String, String> e : res.datos().entrySet()) {
            sb.append("<br>").append(esc(e.getKey())).append(": ").append(esc(e.getValue()));
        }
        datos.add(html(sb.toString()));
        col.add(datos);

        JScrollPane sp = new JScrollPane(col, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private static JPanel pie(JFrame f, AnalizadorCrash.Resultado res, Path juego, List<Path> adjuntos) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        botones.setOpaque(false);
        Boton abrir = new Boton("Abrir carpeta");
        abrir.addActionListener(e -> abrirCarpeta(adjuntos.isEmpty() ? juego : adjuntos.get(0).getParent()));
        Boton copiar = new Boton("Copiar resumen");
        copiar.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(textoPlano(res)), null));
        Boton cerrar = new Boton("Cerrar");
        cerrar.addActionListener(e -> f.dispose());
        botones.add(abrir);
        botones.add(copiar);
        botones.add(cerrar);

        JLabel creditos = new JLabel("El Vigilante " + LanzadorVigilante.VERSION + "  ·  creado por Reapercillo",
                JLabel.CENTER);
        creditos.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        creditos.setForeground(new Color(0x555555));

        p.add(botones, BorderLayout.CENTER);
        p.add(creditos, BorderLayout.SOUTH);
        return p;
    }

    // ---------- piezas de diseño ----------

    /** Ranura oscura con borde negro y franja de color a la izquierda. */
    private static JPanel tarjeta(Color acento) {
        JPanel t = new JPanel() {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setBackground(RANURA);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 5, 0, 0, acento),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10))));
        t.setAlignmentX(0f);
        return t;
    }

    private static JLabel html(String contenido) {
        JLabel l = new JLabel("<html><body style='width:" + ANCHO_TEXTO + "px'>" + contenido + "</body></html>");
        l.setFont(FUENTE);
        l.setForeground(Color.WHITE);
        l.setAlignmentX(0f);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        return l;
    }

    private static Dimension tamEspacio() { return new Dimension(0, 8); }

    private static javax.swing.Box.Filler espacio() {
        return new javax.swing.Box.Filler(tamEspacio(), tamEspacio(), tamEspacio());
    }

    /** Fondo de tierra generado por código, escalado sin suavizar (look pixelado). */
    private static final class Fondo extends JPanel {
        private final BufferedImage tierra = crearTierra();

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int tam = 64;
            for (int y = 0; y < getHeight(); y += tam) {
                for (int x = 0; x < getWidth(); x += tam) {
                    g.drawImage(tierra, x, y, tam, tam, null);
                }
            }
            g.setColor(new Color(0, 0, 0, 110));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        private static BufferedImage crearTierra() {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
            Random r = new Random(7);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    double k = 0.75 + r.nextDouble() * 0.4;
                    int rr = (int) Math.min(255, 0x79 * k);
                    int gg = (int) Math.min(255, 0x55 * k);
                    int bb = (int) Math.min(255, 0x3A * k);
                    img.setRGB(x, y, (rr << 16) | (gg << 8) | bb);
                }
            }
            return img;
        }
    }

    /** Panel gris tipo inventario, con bordes en relieve. */
    private static final class Piedra extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            g.setColor(PIEDRA);
            g.fillRect(0, 0, w, h);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, 3);
            g.fillRect(0, 0, 3, h);
            g.setColor(new Color(0x555555));
            g.fillRect(0, h - 3, w, 3);
            g.fillRect(w - 3, 0, 3, h);
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, w - 1, h - 1);
        }
    }

    /** Botón de menú de Minecraft: gris biselado, se ilumina al pasar el mouse. */
    private static final class Boton extends JButton {
        Boton(String texto) {
            super(texto);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setFont(FUENTE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(180, 36));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth(), h = getHeight();
            boolean sobre = getModel().isRollover();
            boolean presion = getModel().isArmed();
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            g2.setColor(sobre ? new Color(0x7A7FAD) : new Color(0x6F6F6F));
            g2.fillRect(2, 2, w - 4, h - 4);
            g2.setColor(presion ? new Color(0x3C3C3C) : new Color(0xA8A8A8));
            g2.fillRect(2, 2, w - 4, 2);
            g2.fillRect(2, 2, 2, h - 4);
            g2.setColor(presion ? new Color(0xA8A8A8) : new Color(0x3C3C3C));
            g2.fillRect(2, h - 4, w - 4, 2);
            g2.fillRect(w - 4, 2, 2, h - 4);

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String t = getText();
            int x = (w - fm.stringWidth(t)) / 2;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(new Color(0x3F3F3F));
            g2.drawString(t, x + 2, y + 2);
            g2.setColor(sobre ? new Color(0xFFFFA0) : Color.WHITE);
            g2.drawString(t, x, y);
        }
    }

    // ---------- utilidades ----------

    private static String textoPlano(AnalizadorCrash.Resultado res) {
        StringBuilder sb = new StringBuilder("El Vigilante - informe\n");
        sb.append("Loader: ").append(res.loader()).append('\n');
        for (Map.Entry<String, String> e : res.datos().entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        }
        for (AnalizadorCrash.Diagnostico d : res.diagnosticos()) {
            sb.append("\n* ").append(d.titulo()).append("\n  Qué pasó: ").append(d.queParo())
              .append("\n  Solución: ").append(d.solucion()).append('\n');
        }
        if (!res.sospechosos().isEmpty()) {
            sb.append("\nMods sospechosos: ").append(String.join(", ", res.sospechosos())).append('\n');
        }
        return sb.toString();
    }

    private static void abrirCarpeta(Path carpeta) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(carpeta.toFile());
        } catch (Exception ignorado) {}
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private VentanaDiagnostico() {}
}
