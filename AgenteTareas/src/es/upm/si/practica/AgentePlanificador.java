package es.upm.si.practica;

import java.util.ArrayList;
import java.util.List;

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

public class AgentePlanificador extends Agent {

    @Override
    protected void setup() {
        System.out.println("Planificador " + getLocalName() + " iniciado.");

        DFAgentDescription descAgente = new DFAgentDescription();
        descAgente.setName(getAID());
        ServiceDescription descServ = new ServiceDescription();
        descServ.setType("planificacion"); 
        descServ.setName("Servicio-Planificacion");
        descAgente.addServices(descServ);

        try {
            DFService.register(this, descAgente);
            System.out.println(getLocalName() + " registrado en el DF con éxito.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt); 
                
                if (msg != null) {
                    System.out.println(getLocalName() + " ha recibido la petición de la interfaz.");
                    try {
                        // CORRECCIÓN: Casteamos correctamente al objeto que mandó la interfaz
                        FiltrosUsuario filtros = (FiltrosUsuario) msg.getContentObject(); 
                        myAgent.addBehaviour(new GestionarConsultaSensores(msg, filtros));
                    } catch (UnreadableException e) {
                        System.err.println("Error al deserializar los filtros del usuario.");
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }
        });
    }
    
    private class GestionarConsultaSensores extends OneShotBehaviour {
        private ACLMessage mensajeInterfaz;
        private FiltrosUsuario filtros;

        public GestionarConsultaSensores(ACLMessage mensajeInterfaz, FiltrosUsuario filtros) {
            this.mensajeInterfaz = mensajeInterfaz;
            this.filtros = filtros;
        }

        @Override
        public void action() {
            String idConversacion = "conv_" + System.currentTimeMillis();

            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("busqueda-peliculas");
            template.addServices(templateSd);

            List<AID> sensoresEncontrados = new ArrayList<>();
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (int i = 0; i < result.length; ++i) {
                    sensoresEncontrados.add(result[i].getName());
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            if (sensoresEncontrados.isEmpty()) {
                enviarRespuestaError("No hay servicios de búsqueda disponibles en el DF.");
                return;
            }

            ACLMessage requestSensores = new ACLMessage(ACLMessage.REQUEST);
            requestSensores.setConversationId(idConversacion);
            try {
                requestSensores.setContentObject(filtros);
            } catch (Exception e) { e.printStackTrace(); }
            
            for (AID sensorAID : sensoresEncontrados) {
                requestSensores.addReceiver(sensorAID);
            }
            send(requestSensores);
            System.out.println(getLocalName() + ": Petición enviada a los sensores.");

            List<Pelicula> todasLasPeliculas = new ArrayList<>();
            int respuestasRecibidas = 0;
            MessageTemplate mtRespuestas = MessageTemplate.and(MessageTemplate.MatchConversationId(idConversacion),MessageTemplate.MatchPerformative(ACLMessage.INFORM));

            while (respuestasRecibidas < sensoresEncontrados.size()) {
                ACLMessage respuestaSensor = myAgent.blockingReceive(mtRespuestas, 5000);
                if (respuestaSensor != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        ArrayList<Pelicula> pelisSensor = (ArrayList<Pelicula>) respuestaSensor.getContentObject();
                        if (pelisSensor != null) todasLasPeliculas.addAll(pelisSensor);
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                    respuestasRecibidas++;
                } else break;
            }

            ArrayList<Pelicula> rankingFinal = AlgoritmoPlanificador.generarListaPelis(todasLasPeliculas, filtros);

            ACLMessage reply = mensajeInterfaz.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            try {
                reply.setContentObject(rankingFinal); 
                send(reply);
                System.out.println(getLocalName() + " ha enviado el ranking final a la interfaz.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void enviarRespuestaError(String motivo) {
            ACLMessage reply = mensajeInterfaz.createReply();
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent(motivo);
            send(reply);
        }
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException fe) { fe.printStackTrace(); }
    }
}