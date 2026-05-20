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
        System.out.println("[AgenteInterfaz] " + getLocalName() + " iniciado.");
        SwingUtilities.invokeLater(() -> {
            gui = new BuscadorGUI(this);
            gui.mostrar();
        });
    }

    public void comenzarBusqueda(String genero, int anio) {
        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                DFAgentDescription desc = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("planificacion");
                desc.addServices(sd);

                AID planificadorAID = null;
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, desc);
                    if (result.length > 0) planificadorAID = result[0].getName();
                } catch (FIPAException fe) { fe.printStackTrace(); }

                if (planificadorAID == null) {
                    gui.mostrarError("Planificador no encontrado en el DF.");
                    return;
                }

                FiltrosUsuario filtros = new FiltrosUsuario(genero, anio);
                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(planificadorAID);
                try { request.setContentObject(filtros); } catch (Exception e) { e.printStackTrace(); }

                String convId = "planificacion-" + System.currentTimeMillis();
                request.setConversationId(convId);
                send(request);

                MessageTemplate mt = MessageTemplate.MatchConversationId(convId);
                ACLMessage reply = blockingReceive(mt, 10000);

                if (reply != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        ArrayList<Pelicula> ranking = (ArrayList<Pelicula>) reply.getContentObject();
                        gui.mostrarPeliculas(ranking);
                    } catch (UnreadableException e) {
                        gui.mostrarError("Error decodificando la respuesta del planificador.");
                    }
                } else {
                    gui.mostrarError("Timeout: el planificador no respondio a tiempo.");
                }
            }
        });
    }
}
