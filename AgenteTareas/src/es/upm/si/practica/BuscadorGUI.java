package es.upm.si.practica;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

public class BuscadorGUI {
    private AgenteInterfaz myAgent;
    private JFrame frame;
    private JTextField txtGenero;
    private JTextField txtAnio;
    private JEditorPane areaResultados;

    public BuscadorGUI(AgenteInterfaz agent) {
        this.myAgent = agent;
    }

    public void mostrar() {
        frame = new JFrame("Recomendador Semántico de Cine - SMA");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 550);
        frame.setLayout(new BorderLayout());

        JPanel panelNorte = new JPanel(new GridLayout(2, 1));
        panelNorte.setBackground(new Color(41, 6, 40)); 
        
        JPanel panelInputs = new JPanel();
        panelInputs.setOpaque(false);
        JLabel lblGenero = new JLabel("Género / Palabras clave: ");
        lblGenero.setForeground(Color.WHITE);
        txtGenero = new JTextField(15);
        
        JLabel lblAnio = new JLabel("  Año mínimo (Ej: 2010): ");
        lblAnio.setForeground(Color.WHITE);
        txtAnio = new JTextField(5);
        
        JButton btnAnalizar = new JButton("Buscar Películas");

        panelInputs.add(lblGenero);
        panelInputs.add(txtGenero);
        panelInputs.add(lblAnio);
        panelInputs.add(txtAnio);
        panelInputs.add(btnAnalizar);
        
        panelNorte.add(panelInputs);

        areaResultados = new JEditorPane();
        areaResultados.setContentType("text/html");
        areaResultados.setEditable(false);
        JScrollPane scroll = new JScrollPane(areaResultados);

        areaResultados.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (Desktop.isDesktopSupported()) {
                    try { Desktop.getDesktop().browse(e.getURL().toURI()); } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
        });

        frame.add(panelNorte, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);

        btnAnalizar.addActionListener(e -> {
            String genero = txtGenero.getText().trim();
            String anioStr = txtAnio.getText().trim();
            if (!genero.isEmpty()) {
                int anio = anioStr.isEmpty() ? 1900 : Integer.parseInt(anioStr);
                mostrarResultado("<html><body style='font-family:sans-serif; margin:10px;'>Enviando petición al Agente Planificador...</body></html>");
                myAgent.comenzarBusqueda(genero, anio);
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void mostrarResultado(String html) {
        areaResultados.setText(html);
    }
}