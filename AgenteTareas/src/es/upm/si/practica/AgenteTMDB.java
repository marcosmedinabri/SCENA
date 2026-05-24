package es.upm.si.practica;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Esto lo tengo que hacer asi porque para buscar en la api, se busca con los numeritos en vez de las palabras, asique un diccionario y ya esta
        Map<String,String> mapafiltros = new HashMap<>();
        mapafiltros.put("acción", "28");
        mapafiltros.put("aventura", "12");
        mapafiltros.put("animación", "16");
        mapafiltros.put("comedia", "35");
        mapafiltros.put("crimen", "80");
        mapafiltros.put("documental", "99");
        mapafiltros.put("drama", "18");
        mapafiltros.put("familiar", "10751");
        mapafiltros.put("fantasía", "14");
        mapafiltros.put("historia", "36");
        mapafiltros.put("terror", "27");
        mapafiltros.put("música", "10402");
        mapafiltros.put("misterio", "9648");
        mapafiltros.put("romance", "10749");
        mapafiltros.put("ciencia ficción", "878");
        mapafiltros.put("ciencia-ficción", "878");
        mapafiltros.put("película de TV", "10770");
        mapafiltros.put("suspense", "53");
        mapafiltros.put("guerra", "10752");



        StringBuilder generosfiltrados = new StringBuilder();

        for (String genero : filtros.getGeneros()) {
            String idGenero = mapafiltros.get(genero.trim().toLowerCase());

            if (idGenero != null) {
                if (generosfiltrados.length() > 0) {
                    generosfiltrados.append(",");
                }
                generosfiltrados.append(idGenero);
            }
        }



        // El string de la url para lo de obtener info de la api y especificar la api key
        String API_KEY = "e11e7daebd9c3bd388a4c6edcc7b6cb9"; // Esta key sera invalidad antes de hacer publico el repositorio obviamente

        String resultado = conseguirInfoApi("https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY + "&with_genres=" + generosfiltrados +"&sort_by=popularity.desc&page=1&language=es-ES");


        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        ConjuntoTMDB respuesta = gson.fromJson(resultado, ConjuntoTMDB.class);
        List<PeliculaTMDB> peliculas = respuesta.getResults(); // Aqui le digo que solo quiero devuelva la lista de pelis, que es lo que me interesa, y ya luego pues se lo mando al planificador o lo que sea, pero bueno


        // Necesito el mapafiltros a la inversa para que los generos de Peliculas no esten como numeros, sino como palabras como tal, que la api devuelve numeros >:(
        Map<String, String> mapaGeneros = new HashMap<>();
        for (Map.Entry<String, String> entrada : mapafiltros.entrySet()) {
            mapaGeneros.put(entrada.getValue(), entrada.getKey());
        }

        ArrayList<Pelicula> listapeliculasfinal = new ArrayList<>();
        for (PeliculaTMDB p : peliculas) {

            // Filtrar por anio lo que dan
            int anioPelicula = obtenerAnioDesdeFecha(p.getFechaEstreno());
            if (anioPelicula < filtros.getAnio()) {
                continue;
            }

            //Cambiar de numero a palabra en lo de los generos
            List<String> generosNombres = new ArrayList<>();
            for (String idGenero : p.getGeneros()) {
                String nombreGenero = mapaGeneros.get(idGenero);
                if (nombreGenero != null) {
                    generosNombres.add(nombreGenero);
                }
            }

            //System.out.println(p);// Esto es solo para testear/debugear que devuelve bien las pelis para el modelo pelis de TMDB :)
            // Y ahora las convierto a formato Pelicula normal y las aniado
            listapeliculasfinal.add(new Pelicula(p.getNombre(), generosNombres, (int) (p.getPuntuacion())*10, p.getSinopsis())); // Habria que cambiar lo de int a float porque hay puntuaciones con decimales, comentar luego en wasap
        }


        return listapeliculasfinal;
    }

    //Metodo auxiliar para ayudar al filtrado por anios
    private int obtenerAnioDesdeFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return Integer.MAX_VALUE; // para no descartar películas sin fecha porsiacaso
        }
        try {
            return Integer.parseInt(fecha.substring(0, 4));// Pillo los 4 primeros numeros del string = solo el anio
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
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
