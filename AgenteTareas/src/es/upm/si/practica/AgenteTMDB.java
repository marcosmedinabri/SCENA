package es.upm.si.practica;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

        // Se que asi poner la api esta mal, pero se puede resetear y para hacer pruebas de momento asi xd
        // De momomento solo devuelve peliculas de accion, tengo que cambiar esto lo se pero es para probar que funciona bien
        String resultado = conseguirInfoApi("https://api.themoviedb.org/3/discover/movie?api_key=e11e7daebd9c3bd388a4c6edcc7b6cb9&with_genres=28&sort_by=popularity.desc&page=1");


        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        ConjuntoTMDB respuesta = gson.fromJson(resultado, ConjuntoTMDB.class);
        List<PeliculaTMDB> peliculas = respuesta.getResults(); // Aqui le digo que solo quiero devuelva la lista de pelis, que es lo que me interesa, y ya luego pues se lo mando al planificador o lo que sea, pero bueno

        ArrayList<Pelicula> listapeliculasfinal = new ArrayList<>();
        for (PeliculaTMDB p : peliculas) {
            System.out.println(p);// Esto es solo para testear/debugear que devuelve bien las pelis para el modelo pelis de TMDB :)
            // Y ahora las convierto a formato Pelicula normal y las aniado
            listapeliculasfinal.add(new Pelicula(p.getNombre(), p.getGeneros(), (int) p.getPuntuacion())); // Habria que cambiar lo de int a float porque hay puntuaciones con decimales, comentar luego en wasap
        }


        return listapeliculasfinal;
    }

    // Es una clase aux para como bien dice el nombre, obtener el resultado de la llamada api, tal vez deberiamos mover a una clase auxiliar no se
    public static String conseguirInfoApi(String url) {

        URL pagina = null;// Esto para cargar la url
        StringBuilder respuesta = new StringBuilder();
        String lineac;
        try {

            pagina = new URL(url);
            URLConnection conexion = pagina.openConnection(); // Para abrir conexion
            BufferedReader bufin = new BufferedReader(new InputStreamReader(conexion.getInputStream()));

            // linea por linea cargo como en C
            while ((lineac = bufin.readLine()) != null) {
                respuesta.append(lineac);
            }

            bufin.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return respuesta.toString();
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