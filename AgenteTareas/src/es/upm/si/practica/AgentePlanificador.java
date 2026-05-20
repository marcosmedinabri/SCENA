package es.upm.si.practica;

import jade.core.Agent;

import jade.core.AID;
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
import java.util.List;


public class AgentePlanificador extends Agent {

    @Override
    protected void setup() {
        System.out.println("Planificador " + getLocalName() + " iniciado.");
        
        /*AQUI EL AGENTE SE REHISTRA EN EL DIRECTORIO FACILITADOR COMO TIPO "plaificacion"*/
        DFAgentDescription descAgente = new DFAgentDescription();
        descAgente.setName(getAID());
        ServiceDescription descServ = new ServiceDescription();
        descServ.setType("planificacion"); //setea el tipo de servicio
        descServ.setName("Servicio-Planificacion");
        descAgente.addServices(descServ);

        try {
            DFService.register(this, descAgente);
            System.out.println(getLocalName() + " registrado en el DF con éxito.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Comportamiento cíclico
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                // Espera las peticiones (REQUEST)
            	
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt); //mensaje del AgenteInterfaz
                
                if (msg != null) {
                    System.out.println(getLocalName() + " ha recibido filtros para procesar de la interfaz");
   
                    try {
						FiltrosUsuario filtrosRecibidos = (FiltrosUsuario)msg.getContentObject(); //se recogen los filtros
						
						myAgent.addBehaviour(new GestionarConsultaSensores(msg, filtrosRecibidos)); //para comunicarse con los buscadores

					} catch (UnreadableException e) {
						System.err.println("Error al deserializar el objeto: el formato de bytes no es válido.");
						e.printStackTrace();
						// Enviar respuesta de error si falla la lectura de los filtrod
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Error en el formato de datos de la petición.");
                        send(reply);
					}
                    
                } else {
                    // Si no hay mensajes, bloqueamos el comportamiento hasta que llegue uno
                    block();
                }
            }
        });
    }
    
    /*buscar sensores, enviarles los filtros,recolectar sus respuestas de forma síncrona/bloqueante y responder a la interfaz*/
    private class GestionarConsultaSensores extends OneShotBehaviour {
        private ACLMessage mensajeInterfaz;
        private FiltrosUsuario filtros;

        public GestionarConsultaSensores(ACLMessage mensajeInterfaz, FiltrosUsuario filtros) {
            this.mensajeInterfaz = mensajeInterfaz;
            this.filtros = filtros;
        }

        @Override
        public void action() {
            // Generamos un ID único para esta conversación con los sensores
            String idConversacion = "conv_" + System.currentTimeMillis();

            //Buscar sensores en el DF
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
                System.out.println(getLocalName() + ": No se han encontrado sensores disponibles.");
                enviarRespuestaError("No hay servicios de búsqueda disponibles en este momento.");
                return;
            }

            //Enviar la petición de filtrado a cada sensor encontrado
            ACLMessage requestSensores = new ACLMessage(ACLMessage.REQUEST);
            requestSensores.setConversationId(idConversacion);
            try {
            	requestSensores.setContentObject(filtros);
            }catch(Exception e){
            	System.err.println("Error al serializar filtros.");
                e.printStackTrace();
                enviarRespuestaError("Error al procesar los filtros.");
                return;
            }
            
            for (AID sensorAID : sensoresEncontrados) {
                requestSensores.addReceiver(sensorAID);
            }
            send(requestSensores);
            System.out.println(getLocalName() + ": Petición enviada a " + sensoresEncontrados.size() + " sensores.");

            // Recolectar las listas devueltas por los sensores
            List<Pelicula> todasLasPeliculas = new ArrayList<>();
            int respuestasRecibidas = 0;

            // Filtro estricto para capturar solo los mensajes asociados a esta consulta exacta
            MessageTemplate mtRespuestas = MessageTemplate.and(MessageTemplate.MatchConversationId(idConversacion),MessageTemplate.MatchPerformative(ACLMessage.INFORM));

            while (respuestasRecibidas < sensoresEncontrados.size()) {
                // Bloquea este comportamiento secundario hasta recibir respuesta de un sensor
                ACLMessage respuestaSensor = myAgent.blockingReceive(mtRespuestas);
                if (respuestaSensor != null) {
                    try {
                    	@SuppressWarnings("unchecked")
                        // Cada sensor devuelve un ArrayList<Pelicula>
                        List<Pelicula> pelisSensor = (List<Pelicula>) respuestaSensor.getContentObject();
                        if (pelisSensor != null) {
                            todasLasPeliculas.addAll(pelisSensor);
                        }
                    } catch (UnreadableException e) {
                        System.err.println("Error al leer la lista de películas de un sensor.");
                    }
                    respuestasRecibidas++;
                }
            }

            /*
            TODO: Hacer aqui la llamada a un agente (ej: AgenteProcesaPreferencias) para procesar la frase de preferencias del usuario y convertirlas
            en palabras claves para el algoritmo de planificador del ranking
             */



            // Aplicar el algoritmo inteligente, que tendra en cuenta las palabras claves del usuario de preferencias, valoraciones y generos de la peli
            // Cambiamos el String por la lista final tipada que procesará tu GUI
            AlgoritmoPlanificador.generarListaPelis(todasLasPeliculas, filtros);
            
            // Enviar el resultado final empaquetado de vuelta al AgenteInterfaz
            ACLMessage reply = mensajeInterfaz.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            try {
                reply.setContentObject((ArrayList<Pelicula>)todasLasPeliculas); // Enviamos el objeto binario estructurado
                send(reply);
                System.out.println(getLocalName() + " ha enviado el ranking final fusionado a la interfaz.");
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

    // desregistrar el agente cuando se destruye
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("Planificador " + getLocalName() + " desregistrado del DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}