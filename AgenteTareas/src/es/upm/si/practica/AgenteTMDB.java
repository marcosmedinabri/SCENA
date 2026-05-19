package es.upm.si.practica;

import jade.core.Agent;


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
        
        // Obtenemos el primer género solicitado de la lista (por simplicidad en la prueba)
        String generoBuscado = (filtros.getGeneros() != null && !filtros.getGeneros().isEmpty()) 
                ? filtros.getGeneros().get(0) 
                : "General";

        // Añadimos datos de prueba dinámicos concatenando el género y año elegidos por el usuario
        lista.add(new Pelicula("The TMDB " + generoBuscado + " Masterpiece", 8.7, "TMDB"));
        lista.add(new Pelicula("Crónicas de " + filtros.getAnio(), 7.4, "TMDB"));
        lista.add(new Pelicula("Aventura Espacial en " + generoBuscado, 6.2, "TMDB"));
        
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