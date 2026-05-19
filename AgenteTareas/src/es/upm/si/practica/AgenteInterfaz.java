package es.upm.si.practica;

import java.util.ArrayList;

import javax.swing.SwingUtilities;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class AgenteInterfaz extends Agent {

    private BuscadorGUI gui;

    @Override
    protected void setup() {
        System.out.println("Agente Interfaz " + getLocalName() + " iniciado.");
        
        // Arrancamos la interfaz gráfica (GUI) en el hilo de Swing
        SwingUtilities.invokeLater(() -> {
            gui = new BuscadorGUI(this);
            gui.mostrar();
        });
    }

    // Método llamado por la GUI cuando el usuario hace clic en el botón
    public void comenzarBusqueda(String genero, int anio) {
        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                
                // 1. Buscar en el Directorio Facilitador al agente Planificador
                DFAgentDescription desc = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("planificacion");
                desc.addServices(sd);
                
                AID planificadorAID = null;
                
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, desc);
                    if (result.length > 0) {
                        planificadorAID = result[0].getName();
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                
                if (planificadorAID != null) {
                    // 2. Empaquetar los datos del usuario usando la clase FiltrosUsuario
                    FiltrosUsuario filtros = new FiltrosUsuario(genero, anio);

                    // 3. Preparar y enviar el mensaje REQUEST
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(planificadorAID);
                    try {
                        request.setContentObject(filtros); // Enviamos el objeto serializado
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    String convId = "planificacion-" + System.currentTimeMillis();
                    request.setConversationId(convId); 

                    send(request);
                    System.out.println(getLocalName() + " ha enviado los filtros al Planificador.");

                    // 4. Recepción bloqueante: Esperamos a que el Planificador fusione todo
                    MessageTemplate mt = MessageTemplate.MatchConversationId(convId);
                    ACLMessage reply = blockingReceive(mt, 8000); // Timeout de 8 segundos por seguridad
                    
                    if (reply != null) {  
                        try {
                            // Extraemos la lista de películas resultante
                            @SuppressWarnings("unchecked")
                            ArrayList<Pelicula> ranking = (ArrayList<Pelicula>) reply.getContentObject();
                            
                            // Renderizamos el HTML para la GUI
                            String htmlFinal = renderizarHTML(ranking);
                            gui.mostrarResultado(htmlFinal);
                            
                        } catch (UnreadableException e) {
                            gui.mostrarResultado("<html><body>❌ Error al decodificar la lista de películas.</body></html>");
                        }
                    } else {
                        gui.mostrarResultado("<html><body>❌ Timeout: El sistema está tardando demasiado en responder.</body></html>");
                    }
                } else {
                    gui.mostrarResultado("<html><body>❌ Error: No se encontró ningún agente planificador activo.</body></html>");
                }
            }
        });
    }

    // Transforma el ArrayList de Peliculas en tarjetas HTML visuales
    private String renderizarHTML(ArrayList<Pelicula> peliculas) {
        if (peliculas == null || peliculas.isEmpty()) {
            return "<html><body><h3 style='color:#CC0000;'>Sin Resultados</h3><p>No se encontraron películas para esos filtros.</p></body></html>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:sans-serif; margin:15px;'>");
        html.append("<h2 style='color:#1A5276; border-bottom: 2px solid #2980B9; padding-bottom:5px;'>🎬 TOP RECOMENDACIONES</h2>");

        for (int i = 0; i < peliculas.size(); i++) {
            Pelicula peli = peliculas.get(i);
            html.append("<div style='background-color:#EBF5FB; border-left:6px solid #2980B9; padding:12px; margin-bottom:12px; border-radius:4px;'>");
            html.append("<b style='font-size:14px; color:#2C3E50;'>#").append(i + 1).append(" - ").append(peli.getNombre()).append("</b><br>");
            html.append("<span style='color:#148F77;'><b>Match Final: ").append(peli.getPuntuacion()).append("%</b></span> | ");
            html.append("<span style='color:#7D6608;'><b>Géneros: ").append(peli.getGeneros().toString()).append("</b></span><br>");
            html.append("</div>");
        }

        html.append("</body></html>");
        return html.toString();
    }
}