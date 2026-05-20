package es.upm.si.practica;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;

public class BuscadorGUI {

    // Paleta
    private static final Color BG_DARK    = new Color(15, 15, 30);
    private static final Color ACCENT     = new Color(220, 50, 50);
    private static final Color ACCENT2    = new Color(255, 200, 60);
    private static final Color TEXT_WHITE = new Color(240, 240, 255);
    private static final Color TEXT_GRAY  = new Color(150, 150, 180);
    private static final Color CARD_BG    = new Color(30, 30, 60);
    private static final Color CARD_BDR   = new Color(55, 55, 95);

    private static final String OMDB_KEY  = "b938e3e8";
    private static final int POSTER_W     = 65;
    private static final int POSTER_H     = 96;

    private static final String[] GENEROS = {
        "fantasia", "accion", "terror", "comedia", "drama",
        "thriller", "suspenso", "aventura", "animacion", "romance",
        "crimen", "misterio", "documental", "guerra", "ciencia ficcion",
        "sci-fi", "oeste", "western", "historia", "biografia"
    };

    private AgenteInterfaz myAgent;
    private JFrame frame;
    private JComboBox<String> cmbGenero;
    private JSpinner spinAnio;
    private JPanel panelResultados;
    private JLabel lblEstado;
    private JButton btnBuscar;

    public BuscadorGUI(AgenteInterfaz agent) { this.myAgent = agent; }

    public void mostrar() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        frame = new JFrame("Recomendador Semantico de Cine - SMA");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(780, 680);
        frame.setMinimumSize(new Dimension(640, 520));
        frame.getContentPane().setBackground(BG_DARK);
        frame.setLayout(new BorderLayout());

        frame.add(crearHeader(),    BorderLayout.NORTH);
        frame.add(crearScroll(),    BorderLayout.CENTER);
        frame.add(crearStatusBar(), BorderLayout.SOUTH);

        mostrarBienvenida();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ── HEADER ──────────────────────────────────────────────────────────────────

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(45, 10, 55), getWidth(), 0, new Color(15, 15, 65)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 3));
        titulos.setOpaque(false);
        JLabel titulo = new JLabel("RECOMENDADOR SEMANTICO DE CINE", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(TEXT_WHITE);
        JLabel sub = new JLabel("Sistema Multi-Agente  |  TasteDive API", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.ITALIC, 12));
        sub.setForeground(TEXT_GRAY);
        titulos.add(titulo); titulos.add(sub);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(80, 40, 100));

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        busqueda.setOpaque(false);

        busqueda.add(lbl("Genero:"));
        cmbGenero = new JComboBox<>(GENEROS);
        cmbGenero.setPreferredSize(new Dimension(155, 30));
        cmbGenero.setFont(new Font("SansSerif", Font.PLAIN, 13));
        busqueda.add(cmbGenero);

        busqueda.add(Box.createHorizontalStrut(8));
        busqueda.add(lbl("Ano minimo:"));
        spinAnio = new JSpinner(new SpinnerNumberModel(2000, 1970, 2024, 1));
        spinAnio.setPreferredSize(new Dimension(80, 30));
        spinAnio.setFont(new Font("SansSerif", Font.PLAIN, 13));
        busqueda.add(spinAnio);

        busqueda.add(Box.createHorizontalStrut(12));
        btnBuscar = crearBoton();
        busqueda.add(btnBuscar);

        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.setOpaque(false);
        norte.add(titulos,  BorderLayout.NORTH);
        norte.add(sep,      BorderLayout.CENTER);
        norte.add(busqueda, BorderLayout.SOUTH);
        header.add(norte, BorderLayout.CENTER);
        return header;
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
                g2.setColor(getModel().isPressed()  ? new Color(160,25,25)
                          : getModel().isRollover() ? new Color(240,65,65)
                          : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(175, 32));
        btn.addActionListener(e -> {
            String genero = (String) cmbGenero.getSelectedItem();
            int anio = (Integer) spinAnio.getValue();
            mostrarCargando();
            setEstado("Consultando agentes... genero=" + genero + ", anio>=" + anio);
            btnBuscar.setEnabled(false);
            myAgent.comenzarBusqueda(genero, anio);
        });
        return btn;
    }

    // ── SCROLL / STATUS ─────────────────────────────────────────────────────────

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
        lblEstado = new JLabel("Sistema listo. Selecciona un genero y pulsa Buscar.");
        lblEstado.setForeground(TEXT_GRAY);
        lblEstado.setFont(new Font("SansSerif", Font.PLAIN, 11));
        bar.add(lblEstado, BorderLayout.WEST);
        JLabel ver = new JLabel("SMA v1.0  |  UPM SI");
        ver.setForeground(new Color(80, 80, 120));
        ver.setFont(new Font("SansSerif", Font.PLAIN, 10));
        bar.add(ver, BorderLayout.EAST);
        return bar;
    }

    // ── ESTADOS DEL PANEL ───────────────────────────────────────────────────────

    private void mostrarBienvenida() {
        panelResultados.removeAll();
        JPanel c = centrado();
        c.add(icono("🎬", 52), gc(0));
        JLabel m = new JLabel("<html><center>Selecciona un genero y pulsa <b>Buscar Peliculas</b></center></html>", SwingConstants.CENTER);
        m.setFont(new Font("SansSerif", Font.PLAIN, 14)); m.setForeground(TEXT_GRAY);
        c.add(m, gc(1));
        panelResultados.add(Box.createVerticalGlue());
        panelResultados.add(c);
        panelResultados.add(Box.createVerticalGlue());
        panelResultados.revalidate(); panelResultados.repaint();
    }

    private void mostrarCargando() {
        panelResultados.removeAll();
        JPanel c = centrado();
        c.add(icono("⏳", 42), gc(0));
        JLabel m = new JLabel("Consultando los agentes...", SwingConstants.CENTER);
        m.setFont(new Font("SansSerif", Font.ITALIC, 13)); m.setForeground(TEXT_GRAY);
        c.add(m, gc(1));
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true); pb.setPreferredSize(new Dimension(260, 8));
        pb.setBackground(CARD_BG); pb.setForeground(ACCENT); pb.setBorderPainted(false);
        c.add(pb, gc(2));
        panelResultados.add(Box.createVerticalGlue());
        panelResultados.add(c);
        panelResultados.add(Box.createVerticalGlue());
        panelResultados.revalidate(); panelResultados.repaint();
    }

    // ── RESULTADOS ──────────────────────────────────────────────────────────────

    public void mostrarPeliculas(ArrayList<Pelicula> peliculas) {
        SwingUtilities.invokeLater(() -> {
            btnBuscar.setEnabled(true);
            panelResultados.removeAll();

            if (peliculas == null || peliculas.isEmpty()) {
                mostrarError("Sin resultados. Prueba otro genero o reduce el ano minimo.");
                setEstado("Busqueda completada — sin resultados.");
                return;
            }

            JLabel enc = new JLabel("  TOP RECOMENDACIONES");
            enc.setFont(new Font("SansSerif", Font.BOLD, 16));
            enc.setForeground(ACCENT2);
            enc.setAlignmentX(Component.LEFT_ALIGNMENT);
            enc.setBorder(new EmptyBorder(4, 0, 10, 0));
            panelResultados.add(enc);

            String[] medallas = {"🥇", "🥈", "🥉", "4.", "5."};
            for (int i = 0; i < peliculas.size(); i++) {
                JPanel card = crearTarjeta(i, peliculas.get(i), medallas);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                panelResultados.add(card);
                panelResultados.add(Box.createVerticalStrut(10));
            }

            panelResultados.add(Box.createVerticalGlue());
            panelResultados.revalidate(); panelResultados.repaint();
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
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, POSTER_H + 20));

        // ── Poster (izquierda) ──────────────────────────────
        JLabel lblPoster = crearPlaceholderPoster();
        card.add(lblPoster, BorderLayout.WEST);
        cargarPosterAsync(p.getNombre(), lblPoster, card);

        // ── Medalla + info (centro) ─────────────────────────
        JPanel centro = new JPanel(new BorderLayout(8, 0));
        centro.setOpaque(false);

        String medalla = idx < medallas.length ? medallas[idx] : (idx+1) + ".";
        JLabel lblMed = new JLabel(medalla, SwingConstants.CENTER);
        lblMed.setFont(new Font("SansSerif", Font.PLAIN, 24));
        lblMed.setPreferredSize(new Dimension(42, POSTER_H));
        centro.add(lblMed, BorderLayout.WEST);

        JPanel info = new JPanel(new GridLayout(2, 1, 0, 6));
        info.setOpaque(false);
        JLabel lblNombre = new JLabel(p.getNombre());
        lblNombre.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblNombre.setForeground(TEXT_WHITE);
        String fuente = (p.getGeneros() != null && !p.getGeneros().isEmpty()) ? p.getGeneros().get(0) : "—";
        JLabel lblFuente = new JLabel("Fuente: " + fuente);
        lblFuente.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblFuente.setForeground(TEXT_GRAY);
        info.add(lblNombre); info.add(lblFuente);
        centro.add(info, BorderLayout.CENTER);
        card.add(centro, BorderLayout.CENTER);

        // ── Puntuacion (derecha) ────────────────────────────
        int pnt = p.getPuntuacion();
        Color colPnt = pnt >= 80 ? new Color(80,200,90) : pnt >= 60 ? new Color(255,195,50) : new Color(240,80,80);
        JPanel derecha = new JPanel(new GridLayout(2, 1, 0, 2));
        derecha.setOpaque(false);
        derecha.setPreferredSize(new Dimension(72, POSTER_H));
        JLabel lblPnt = new JLabel(pnt + "%", SwingConstants.CENTER);
        lblPnt.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblPnt.setForeground(colPnt);
        JLabel lblMatch = new JLabel("Match", SwingConstants.CENTER);
        lblMatch.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblMatch.setForeground(TEXT_GRAY);
        derecha.add(lblPnt); derecha.add(lblMatch);
        card.add(derecha, BorderLayout.EAST);

        return card;
    }

    // ── POSTER ASYNC ────────────────────────────────────────────────────────────

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

    private void cargarPosterAsync(String titulo, JLabel lblPoster, JPanel card) {
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    String urlStr = "https://www.omdbapi.com/?t="
                        + URLEncoder.encode(titulo, "UTF-8") + "&apikey=" + OMDB_KEY;
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
                    if (conn.getResponseCode() != 200) return null;

                    Scanner sc = new Scanner(conn.getInputStream(), "UTF-8");
                    StringBuilder sb = new StringBuilder();
                    while (sc.hasNextLine()) sb.append(sc.nextLine());
                    sc.close();
                    String json = sb.toString();

                    // Extraer URL del poster
                    int pi = json.indexOf("\"Poster\":\"");
                    if (pi == -1) return null;
                    String posterUrl = json.substring(pi + 10, json.indexOf("\"", pi + 10));
                    if (posterUrl.equals("N/A") || posterUrl.isEmpty()) return null;

                    // Descargar imagen
                    HttpURLConnection imgConn = (HttpURLConnection) new URL(posterUrl).openConnection();
                    imgConn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    imgConn.setConnectTimeout(6000); imgConn.setReadTimeout(6000);
                    InputStream is = imgConn.getInputStream();
                    BufferedImage img = ImageIO.read(is);
                    is.close();
                    if (img == null) return null;

                    // Escalar manteniendo proporcion
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
                        card.revalidate(); card.repaint();
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ── ERROR ────────────────────────────────────────────────────────────────────

    public void mostrarError(String msg) {
        SwingUtilities.invokeLater(() -> {
            btnBuscar.setEnabled(true);
            panelResultados.removeAll();
            JPanel c = centrado();
            c.add(icono("⚠️", 36), gc(0));
            JLabel txt = new JLabel("<html><center>" + msg + "</center></html>", SwingConstants.CENTER);
            txt.setFont(new Font("SansSerif", Font.PLAIN, 13));
            txt.setForeground(new Color(220,90,90));
            txt.setPreferredSize(new Dimension(400, 50));
            c.add(txt, gc(1));
            panelResultados.add(Box.createVerticalGlue());
            panelResultados.add(c);
            panelResultados.add(Box.createVerticalGlue());
            panelResultados.revalidate(); panelResultados.repaint();
            setEstado("Error: " + msg);
        });
    }

    // ── UTILIDADES ──────────────────────────────────────────────────────────────

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
        gc.gridx = 0; gc.gridy = row; gc.insets = new Insets(6, 6, 6, 6);
        return gc;
    }

    private void setEstado(String msg) {
        SwingUtilities.invokeLater(() -> lblEstado.setText(msg));
    }
}
