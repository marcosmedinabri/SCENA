package es.upm.si.practica;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

public class AgenteInterfaz extends Agent {

    private BuscadorGUI gui;

    @Override
    protected void setup() {
        System.out.println("[AgenteInterfaz] " + getLocalName() + " iniciado.");
        SwingUtilities.invokeLater(() -> {
            gui = new BuscadorGUI(this);
            gui.mostrar();
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
                } catch (UnreadableException e) {
                    gui.mostrarError("Error al decodificar la respuesta del planificador.");
                    e.printStackTrace();
                }
            }
        });
    }
}
