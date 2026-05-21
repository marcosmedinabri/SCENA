package es.upm.si.practica;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

public class AgenteInterfaz extends Agent {

    private volatile BuscadorGUI gui;

    /** Mapea convId -> titulo para poder actualizar el poster correcto al recibir respuesta */
    private final Map<String, String> pendingPosters = new HashMap<>();

    @Override
    protected void setup() {
        System.out.println("[AgenteInterfaz] " + getLocalName() + " iniciado.");
        SwingUtilities.invokeLater(() -> {
            gui = new BuscadorGUI(this);
            gui.mostrar();
        });

        // Comportamiento permanente: recibe respuestas de poster de AgenteOMDB
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    String convId = msg.getConversationId();
                    if (convId != null && convId.startsWith("poster-")) {
                        String titulo = pendingPosters.remove(convId);
                        if (titulo != null && gui != null) {
                            gui.actualizarPosterDesdeUrl(titulo, msg.getContent());
                        }
                    }
                    // Mensajes INFORM que no son de poster se ignoran (no deberían llegar aquí)
                } else {
                    block();
                }
            }
        });
    }

    /**
     * Llamado por la GUI cuando el usuario pulsa "Buscar Peliculas".
     * Lanza un OneShotBehaviour que busca al planificador en el DF,
     * le envía los filtros y espera la respuesta para mostrársela en la GUI.
     */
    public void comenzarBusqueda(List<String> generos, int anio, String preferencia) {
        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                // 1. Buscar el agente planificador en el Directory Facilitator
                DFAgentDescription desc = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("planificacion");
                desc.addServices(sd);

                AID planificadorAID = null;
                try {
                    System.out.println("[AgenteInterfaz] Buscando agente planificador en el DF...");
                    DFAgentDescription[] result = DFService.search(myAgent, desc);
                    if (result.length > 0) {
                        planificadorAID = result[0].getName();
                        System.out.println("[AgenteInterfaz] Planificador encontrado: " + planificadorAID.getLocalName());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                if (planificadorAID == null) {
                    gui.mostrarError("Planificador no encontrado en el DF. Asegurate de que esta en marcha.");
                    return;
                }

                // 2. Construir los filtros y enviarlos al planificador
                FiltrosUsuario filtros = new FiltrosUsuario(generos, anio, preferencia);

                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(planificadorAID);
                try {
                    request.setContentObject(filtros);
                } catch (IOException e) {
                    e.printStackTrace();
                    gui.mostrarError("Error al empaquetar los filtros.");
                    return;
                }

                String convId = "planificacion-" + System.currentTimeMillis();
                request.setConversationId(convId);
                send(request);
                System.out.println("[AgenteInterfaz] Filtros enviados (convId=" + convId + "). Esperando respuesta...");

                // 3. Esperar la respuesta del planificador (bloqueante, con timeout de 15 s)
                MessageTemplate mt = MessageTemplate.MatchConversationId(convId);
                ACLMessage reply = blockingReceive(mt, 15000);

                if (reply == null) {
                    gui.mostrarError("Timeout: el planificador no respondio a tiempo.");
                    return;
                }

                if (reply.getPerformative() == ACLMessage.FAILURE) {
                    gui.mostrarError("El planificador reporto un error: " + reply.getContent());
                    return;
                }

                // 4. Mostrar el ranking recibido en la GUI
                try {
                    @SuppressWarnings("unchecked")
                    ArrayList<Pelicula> ranking = (ArrayList<Pelicula>) reply.getContentObject();
                    gui.mostrarPeliculas(ranking);
                    System.out.println("[AgenteInterfaz] Ranking recibido con "
                        + (ranking != null ? ranking.size() : 0) + " peliculas.");

                    // 5. Solicitar posters a AgenteOMDB para cada película del ranking
                    if (ranking != null && !ranking.isEmpty()) {
                        DFAgentDescription descOMDB = new DFAgentDescription();
                        ServiceDescription sdOMDB = new ServiceDescription();
                        sdOMDB.setType("busqueda-posters");
                        descOMDB.addServices(sdOMDB);

                        AID omdbAID = null;
                        try {
                            DFAgentDescription[] resOMDB = DFService.search(myAgent, descOMDB);
                            if (resOMDB.length > 0) {
                                omdbAID = resOMDB[0].getName();
                            }
                        } catch (FIPAException fe) {
                            System.err.println("[AgenteInterfaz] No se encontró AgenteOMDB en el DF.");
                        }

                        if (omdbAID != null) {
                            long ts = System.currentTimeMillis();
                            for (int i = 0; i < ranking.size(); i++) {
                                String titulo = ranking.get(i).getNombre();
                                String posterConvId = "poster-" + ts + "-" + i;
                                ACLMessage posterReq = new ACLMessage(ACLMessage.REQUEST);
                                posterReq.addReceiver(omdbAID);
                                posterReq.setContent(titulo);
                                posterReq.setConversationId(posterConvId);
                                send(posterReq);
                                pendingPosters.put(posterConvId, titulo);
                            }
                            System.out.println("[AgenteInterfaz] Enviadas " + ranking.size()
                                + " peticiones de poster a AgenteOMDB.");
                        }
                    }
                } catch (UnreadableException e) {
                    gui.mostrarError("Error al decodificar la respuesta del planificador.");
                    e.printStackTrace();
                }
            }
        });
    }
}
