package es.upm.si.practica;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class AgenteTMDB extends Agent {

    @Override
    protected void setup() {
    	System.out.println("AgenteTMDB " + getLocalName() + " iniciado.");

        /*AQUI EL AGENTE SE REGISTRA EN EL DIRECTORIO FACILITADOR COMO TIPO "busqueda-peliculas"*/ 	
        DFAgentDescription descAgente = new DFAgentDescription();
        descAgente.setName(getAID());
        ServiceDescription descServ = new ServiceDescription();
        descServ.setType("busqueda-peliculas"); //setea el tipo de servicio
        descServ.setName("servicio-tmdb");
        descAgente.addServices(descServ);

        try {
            DFService.register(this, descAgente); //Se registra en las paginas amarillas
            System.out.println(getLocalName() + " registrado en el DF con éxito.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        /* 2. COMPORTAMIENTO CÍCLICO PARA ATENDER PETICIONES */
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                // Filtrar para recibir únicamente mensajes del tipo REQUEST
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    System.out.println(getLocalName() + ": Ha recibido una petición de búsqueda del Planificador.");
                    
                    try {
                        // Extraer los filtros enviados por el Planificador
                        FiltrosUsuario filtros = (FiltrosUsuario) msg.getContentObject();
                        System.out.println(getLocalName() + ": Procesando filtros -> " + filtros);

                        // Generar la lista de películas de prueba simulando la consulta a la API de TMDB
                        ArrayList<Pelicula> pelisSimuladas = generarPeliculasDePrueba(filtros);

                        // Preparar la respuesta estructurada
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        
                        // CRUCIAL: El ConversationId debe ser el mismo para mantener la trazabilidad asíncrona
                        reply.setConversationId(msg.getConversationId()); 
                        
                        // Serializar y empaquetar el objeto binario de respuesta
                        reply.setContentObject(pelisSimuladas);
                        send(reply);
                        
                        System.out.println(getLocalName() + ": Enviadas " + pelisSimuladas.size() + " películas de prueba al Planificador.");

                    } catch (UnreadableException ue) {
                        System.err.println(getLocalName() + ": Error crítico al deserializar los filtros del Planificador.");
                        ue.printStackTrace();
                    } catch (IOException ioe) {
                        System.err.println(getLocalName() + ": Error al serializar la lista de películas simuladas.");
                        ioe.printStackTrace();
                    }
                } else {
                    block(); // Bloquear comportamiento para liberar la CPU si no hay mensajes en la cola
                }
            }
        });
    }
    
    private ArrayList<Pelicula> generarPeliculasDePrueba(FiltrosUsuario filtros) {
        ArrayList<Pelicula> lista = new ArrayList<>();
        
        // 1. Obtenemos el primer género solicitado de la lista (por simplicidad en la prueba)
        String generoBuscado = (filtros.getGeneros() != null && !filtros.getGeneros().isEmpty()) 
                ? filtros.getGeneros().get(0) 
                : "General";

        // 2. Preparamos una lista de géneros simulada que contenga el género buscado y la fuente "TMDB"
        List<String> generosSimulados = new ArrayList<>();
        generosSimulados.add(generoBuscado);
        generosSimulados.add("TMDB"); // Añadimos la procedencia como un tag de género provisional

        // 3. Añadimos los datos de prueba usando estrictamente tu constructor original: (String, List<String>, int)
        lista.add(new Pelicula("The TMDB " + generoBuscado + " Masterpiece", generosSimulados, 8));
        lista.add(new Pelicula("Crónicas de " + filtros.getAnio(), generosSimulados, 7));
        lista.add(new Pelicula("Aventura Espacial en " + generoBuscado, generosSimulados, 6));
        
        return lista;
    }
    
 // desregistrar el agente cuando se destruye
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("AgenteTMDB " + getLocalName() + " desregistrado del DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

}