package es.upm.si.practica;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;

public class BuscadorGUI {

    // ── Paleta ───────────────────────────────────────────────────────────────────
    private static final Color BG_DARK    = new Color(15, 15, 30);
    private static final Color ACCENT     = new Color(220, 50, 50);
    private static final Color ACCENT2    = new Color(255, 200, 60);
    private static final Color TEXT_WHITE = new Color(240, 240, 255);
    private static final Color TEXT_GRAY  = new Color(150, 150, 180);
    private static final Color CARD_BG    = new Color(30, 30, 60);
    private static final Color CARD_BDR   = new Color(55, 55, 95);
    private static final Color INPUT_BG   = new Color(25, 25, 50);
    private static final Color INPUT_BDR  = new Color(70, 70, 110);

    private static final int POSTER_W = 65;
    private static final int POSTER_H = 96;

    private AgenteInterfaz myAgent;
    private JFrame frame;
    private JTextField txtGeneros;
    private JTextField txtPreferencia;
    private JSpinner spinAnio;
    private JPanel panelResultados;
    private JLabel lblEstado;
    private JButton btnBuscar;

    /** Mapa titulo -> JLabel del poster para actualización asíncrona desde AgenteOMDB */
    private final Map<String, JLabel> mapaPosterLabels = new HashMap<>();

    public BuscadorGUI(AgenteInterfaz agent) {
        this.myAgent = agent;
    }

    public void mostrar() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        frame = new JFrame("Recomendador Semantico de Cine - SMA");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(820, 720);
        frame.setMinimumSize(new Dimension(660, 540));
        frame.getContentPane().setBackground(BG_DARK);
        frame.setLayout(new BorderLayout());

        frame.add(crearHeader(),    BorderLayout.NORTH);
        frame.add(crearScroll(),    BorderLayout.CENTER);
        frame.add(crearStatusBar(), BorderLayout.SOUTH);

        mostrarBienvenida();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ── HEADER ───────────────────────────────────────────────────────────────────

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(45, 10, 55), getWidth(), 0, new Color(15, 15, 65)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(18, 28, 18, 28));

        // Título
        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 3));
        titulos.setOpaque(false);
        JLabel titulo = new JLabel("RECOMENDADOR SEMANTICO DE CINE", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(TEXT_WHITE);
        JLabel sub = new JLabel("Sistema Multi-Agente  |  TMDB API", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.ITALIC, 12));
        sub.setForeground(TEXT_GRAY);
        titulos.add(titulo);
        titulos.add(sub);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(80, 40, 100));

        // Panel de búsqueda con 3 filas de campos
        JPanel busqueda = new JPanel(new GridBagLayout());
        busqueda.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 1: Géneros
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        busqueda.add(lbl("Generos (separados por coma):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtGeneros = crearCampoTexto("ej: accion, drama, thriller...");
        busqueda.add(txtGeneros, gbc);

        // Fila 2: Año + botón
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        busqueda.add(lbl("Ano minimo:"), gbc);
        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fila2.setOpaque(false);
        spinAnio = new JSpinner(new SpinnerNumberModel(2000, 1970, 2025, 1));
        spinAnio.setPreferredSize(new Dimension(85, 30));
        spinAnio.setFont(new Font("SansSerif", Font.PLAIN, 13));
        fila2.add(spinAnio);
        fila2.add(Box.createHorizontalStrut(20));
        btnBuscar = crearBoton();
        fila2.add(btnBuscar);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        busqueda.add(fila2, gbc);

        // Fila 3: Preferencia
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        busqueda.add(lbl("Que quieres ver:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        txtPreferencia = crearCampoTexto("ej: una pelicula de robots en el futuro...");
        busqueda.add(txtPreferencia, gbc);

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        norte.add(titulos,  BorderLayout.NORTH);
        norte.add(sep,      BorderLayout.CENTER);
        norte.add(busqueda, BorderLayout.SOUTH);
        header.add(norte, BorderLayout.CENTER);
        return header;
    }

    private JTextField crearCampoTexto(String placeholder) {
        JTextField tf = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(INPUT_BDR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(4, 10, 4, 10));
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setForeground(TEXT_WHITE);
        tf.setCaretColor(TEXT_WHITE);
        tf.setPreferredSize(new Dimension(0, 30));
        // Placeholder
        tf.setText(placeholder);
        tf.setForeground(TEXT_GRAY);
        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (tf.getText().equals(placeholder)) {
                    tf.setText("");
                    tf.setForeground(TEXT_WHITE);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (tf.getText().isEmpty()) {
                    tf.setText(placeholder);
                    tf.setForeground(TEXT_GRAY);
                }
            }
        });
        return tf;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(TEXT_WHITE);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return l;
    }

    private JButton crearBoton() {
        JButton btn = new JButton("  Buscar Peliculas  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()  ? new Color(160, 25, 25)
                          : getModel().isRollover() ? new Color(240, 65, 65)
                          : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(175, 32));
        btn.addActionListener(e -> lanzarBusqueda());
        return btn;
    }

    private void lanzarBusqueda() {
        // Recoger géneros del campo de texto y parsear por coma
        String textoGeneros = txtGeneros.getText().trim();
        String placeholderGeneros = "ej: accion, drama, thriller...";
        if (textoGeneros.isEmpty() || textoGeneros.equals(placeholderGeneros)) {
            mostrarError("Por favor, introduce al menos un genero.");
            return;
        }
        List<String> listaGeneros = Arrays.stream(textoGeneros.split("\\s*,\\s*"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        int anio = (Integer) spinAnio.getValue();

        String preferencia = txtPreferencia.getText().trim();
        String placeholderPref = "ej: una pelicula de robots en el futuro...";
        if (preferencia.equals(placeholderPref)) preferencia = "";

        mostrarCargando();
        setEstado("Consultando agentes... generos=" + listaGeneros + ", anio>=" + anio);
        btnBuscar.setEnabled(false);

        myAgent.comenzarBusqueda(listaGeneros, anio, preferencia);
    }

    // ── SCROLL / STATUS ──────────────────────────────────────────────────────────

    private JScrollPane crearScroll() {
        panelResultados = new JPanel();
        panelResultados.setLayout(new BoxLayout(panelResultados, BoxLayout.Y_AXIS));
        panelResultados.setBackground(BG_DARK);
        panelResultados.setBorder(new EmptyBorder(12, 24, 12, 24));

        JScrollPane scroll = new JScrollPane(panelResultados);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel crearStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(10, 10, 20));
        bar.setBorder(new EmptyBorder(5, 16, 5, 16));
        lblEstado = new JLabel("Sistema listo. Introduce generos y pulsa Buscar.");
        lblEstado.setForeground(TEXT_GRAY);
        lblEstado.setFont(new Font("SansSerif", Font.PLAIN, 11));
        bar.add(lblEstado, BorderLayout.WEST);
        JLabel ver = new JLabel("SMA v1.0  |  UPM SSII");
        ver.setForeground(new Color(80, 80, 120));
        ver.setFont(new Font("SansSerif", Font.PLAIN, 10));
        bar.add(ver, BorderLayout.EAST);
        return bar;
    }

    // ── ESTADOS DEL PANEL ────────────────────────────────────────────────────────

    private void mostrarBienvenida() {
        panelResultados.removeAll();
        JPanel c = centrado();
        c.add(icono("🎬", 52), gc(0));
        JLabel m = new JLabel(
            "<html><center>Escribe los generos que quieres ver, el ano minimo<br>y una breve descripcion de lo que buscas.</center></html>",
            SwingConstants.CENTER);
        m.setFont(new Font("SansSerif", Font.PLAIN, 14));
        m.setForeground(TEXT_GRAY);
        c.add(m, gc(1));
        panelResultados.add(Box.createVerticalGlue());
        panelResultados.add(c);
        panelResultados.add(Box.createVerticalGlue());
        panelResultados.revalidate();
        panelResultados.repaint();
    }

    private void mostrarCargando() {
        panelResultados.removeAll();
        JPanel c = centrado();
        c.add(icono("⏳", 42), gc(0));
        JLabel m = new JLabel("Consultando los agentes...", SwingConstants.CENTER);
        m.setFont(new Font("SansSerif", Font.ITALIC, 13));
        m.setForeground(TEXT_GRAY);
        c.add(m, gc(1));
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        pb.setPreferredSize(new Dimension(260, 8));
        pb.setBackground(CARD_BG);
        pb.setForeground(ACCENT);
        pb.setBorderPainted(false);
        c.add(pb, gc(2));
        panelResultados.add(Box.createVerticalGlue());
        panelResultados.add(c);
        panelResultados.add(Box.createVerticalGlue());
        panelResultados.revalidate();
        panelResultados.repaint();
    }

    // ── RESULTADOS ───────────────────────────────────────────────────────────────

    public void mostrarPeliculas(ArrayList<Pelicula> peliculas) {
        SwingUtilities.invokeLater(() -> {
            btnBuscar.setEnabled(true);
            mapaPosterLabels.clear();
            panelResultados.removeAll();

            if (peliculas == null || peliculas.isEmpty()) {
                mostrarError("Sin resultados. Prueba otros generos o reduce el ano minimo.");
                setEstado("Busqueda completada — sin resultados.");
                return;
            }

            JLabel enc = new JLabel("  TOP RECOMENDACIONES");
            enc.setFont(new Font("SansSerif", Font.BOLD, 16));
            enc.setForeground(ACCENT2);
            enc.setAlignmentX(Component.LEFT_ALIGNMENT);
            enc.setBorder(new EmptyBorder(4, 0, 10, 0));
            panelResultados.add(enc);

            String[] medallas = {"🥇", "🥈", "🥉", "4.", "5.", "6.", "7.", "8.", "9.", "10."};
            for (int i = 0; i < peliculas.size(); i++) {
                JPanel card = crearTarjeta(i, peliculas.get(i), medallas);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                panelResultados.add(card);
                panelResultados.add(Box.createVerticalStrut(10));
            }

            panelResultados.add(Box.createVerticalGlue());
            panelResultados.revalidate();
            panelResultados.repaint();
            setEstado("Busqueda completada: " + peliculas.size() + " resultado(s).");
        });
    }

    private JPanel crearTarjeta(int idx, Pelicula p, String[] medallas) {
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(CARD_BDR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 16));

        // ── Poster (izquierda) — se actualiza cuando AgenteOMDB responda ──────────
        JLabel lblPoster = crearPlaceholderPoster();
        mapaPosterLabels.put(p.getNombre(), lblPoster);
        card.add(lblPoster, BorderLayout.WEST);

        // ── Medalla + info (centro) ─────────────────────────
        JPanel centro = new JPanel(new BorderLayout(8, 0));
        centro.setOpaque(false);

        String medalla = idx < medallas.length ? medallas[idx] : (idx + 1) + ".";
        JLabel lblMed = new JLabel(medalla, SwingConstants.CENTER);
        lblMed.setFont(new Font("SansSerif", Font.PLAIN, 24));
        lblMed.setPreferredSize(new Dimension(42, POSTER_H));
        centro.add(lblMed, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel lblNombre = new JLabel(p.getNombre());
        lblNombre.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblNombre.setForeground(TEXT_WHITE);
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);

        String generosStr = (p.getGeneros() != null && !p.getGeneros().isEmpty())
            ? String.join(", ", p.getGeneros()) : "—";
        JLabel lblGeneros = new JLabel("Generos: " + generosStr);
        lblGeneros.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblGeneros.setForeground(ACCENT2);
        lblGeneros.setAlignmentX(Component.LEFT_ALIGNMENT);

        String sinopsisText = (p.getSinopsis() != null && !p.getSinopsis().trim().isEmpty())
            ? p.getSinopsis() : "Sin sinopsis disponible.";
        if (sinopsisText.length() > 200) sinopsisText = sinopsisText.substring(0, 197) + "...";
        JLabel lblSinopsis = new JLabel("<html><div style='width:380px'>" + sinopsisText + "</div></html>");
        lblSinopsis.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblSinopsis.setForeground(TEXT_GRAY);
        lblSinopsis.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(lblNombre);
        info.add(Box.createVerticalStrut(3));
        info.add(lblGeneros);
        info.add(Box.createVerticalStrut(5));
        info.add(lblSinopsis);
        centro.add(info, BorderLayout.CENTER);
        card.add(centro, BorderLayout.CENTER);

        return card;
    }

    // ── POSTER ───────────────────────────────────────────────────────────────────

    private JLabel crearPlaceholderPoster() {
        JLabel lbl = new JLabel("🎞", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 20, 45));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 26));
        lbl.setPreferredSize(new Dimension(POSTER_W, POSTER_H));
        lbl.setOpaque(false);
        return lbl;
    }

    /**
     * Llamado por AgenteInterfaz cuando AgenteOMDB devuelve la URL del poster.
     * Descarga la imagen en segundo plano (SwingWorker) y actualiza el label.
     */
    public void actualizarPosterDesdeUrl(String titulo, String posterUrl) {
        JLabel lblPoster = mapaPosterLabels.get(titulo);
        if (lblPoster == null || posterUrl == null) return;

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    HttpURLConnection imgConn = (HttpURLConnection) new URL(posterUrl).openConnection();
                    imgConn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    imgConn.setConnectTimeout(6000);
                    imgConn.setReadTimeout(6000);
                    InputStream is = imgConn.getInputStream();
                    BufferedImage img = ImageIO.read(is);
                    is.close();
                    if (img == null) return null;
                    Image scaled = img.getScaledInstance(POSTER_W, POSTER_H, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        lblPoster.setText(null);
                        lblPoster.setIcon(icon);
                        Container parent = lblPoster.getParent();
                        if (parent != null) {
                            parent.revalidate();
                            parent.repaint();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ── ERROR ─────────────────────────────────────────────────────────────────────

    public void mostrarError(String msg) {
        SwingUtilities.invokeLater(() -> {
            btnBuscar.setEnabled(true);
            panelResultados.removeAll();
            JPanel c = centrado();
            c.add(icono("⚠️", 36), gc(0));
            JLabel txt = new JLabel("<html><center>" + msg + "</center></html>", SwingConstants.CENTER);
            txt.setFont(new Font("SansSerif", Font.PLAIN, 13));
            txt.setForeground(new Color(220, 90, 90));
            txt.setPreferredSize(new Dimension(420, 60));
            c.add(txt, gc(1));
            panelResultados.add(Box.createVerticalGlue());
            panelResultados.add(c);
            panelResultados.add(Box.createVerticalGlue());
            panelResultados.revalidate();
            panelResultados.repaint();
            setEstado("Error: " + msg);
        });
    }

    // ── UTILIDADES ───────────────────────────────────────────────────────────────

    private JPanel centrado() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_DARK);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        return p;
    }

    private JLabel icono(String emoji, int size) {
        JLabel l = new JLabel(emoji, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.PLAIN, size));
        return l;
    }

    private GridBagConstraints gc(int row) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = row;
        gc.insets = new Insets(6, 6, 6, 6);
        return gc;
    }

    private void setEstado(String msg) {
        SwingUtilities.invokeLater(() -> lblEstado.setText(msg));
    }
}
