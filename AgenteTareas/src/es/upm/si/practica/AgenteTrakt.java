package es.upm.si.practica;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

// Este agente consulta la API de Trakt.tv para buscar películas según los filtros que le manda el Planificador.
// Trakt tiene dos endpoints útiles: uno de búsqueda por título y otro de películas populares por género.
// Dependiendo de si el usuario pide un género conocido o una keyword libre, uso uno u otro para no devolver basura.
public class AgenteTrakt extends Agent {

    // Clave de la API de Trakt — hay que mandarla como cabecera en cada petición o devuelve 401.
    private static final String CLIENT_ID = "181dd5e515e1be4d9392dab504f7ce858e4678c8c4aa8c5e5d1c96a1eba389b8";

    // Diccionario para traducir géneros en español (o ya en inglés) que acepta la API de Trakt.
    // Pongo tanto la versión con tilde como sin ella porque los usuarios escriben de todo y que no falle.
    // También los meto directamente en inglés por si acaso llega algo así del planificador.
    private static final Map<String, String> GENEROS_EN = new HashMap<>();
    static {
        GENEROS_EN.put("acción",    "action");   GENEROS_EN.put("accion",     "action");
        GENEROS_EN.put("terror",    "horror");   GENEROS_EN.put("horror",     "horror");
        GENEROS_EN.put("comedia",   "comedy");   GENEROS_EN.put("comedy",     "comedy");
        GENEROS_EN.put("drama",     "drama");    GENEROS_EN.put("thriller",   "thriller");
        GENEROS_EN.put("aventura",  "adventure");GENEROS_EN.put("adventure",  "adventure");
        GENEROS_EN.put("animación", "animation");GENEROS_EN.put("animacion",  "animation");
        GENEROS_EN.put("romance",   "romance");  GENEROS_EN.put("fantasía",   "fantasy");
        GENEROS_EN.put("fantasia",  "fantasy");  GENEROS_EN.put("fantasy",    "fantasy");
        GENEROS_EN.put("crimen",    "crime");    GENEROS_EN.put("crime",      "crime");
        GENEROS_EN.put("misterio",  "mystery");  GENEROS_EN.put("mystery",    "mystery");
        GENEROS_EN.put("documental","documentary");GENEROS_EN.put("guerra",   "war");
        GENEROS_EN.put("ciencia ficción","science-fiction");GENEROS_EN.put("ciencia ficcion","science-fiction");
        GENEROS_EN.put("sci-fi",    "science-fiction");       GENEROS_EN.put("sci fi",      "science-fiction");
        GENEROS_EN.put("oeste",   "western");  GENEROS_EN.put("western",     "western");
        GENEROS_EN.put("suspenso",  "thriller");  GENEROS_EN.put("accion",    "action");
    }

    @Override
        protected void setup() {
    	System.out.println("AgenteTrakt " + getLocalName() + " iniciado.");

        /*AQUI EL AGENTE SE REGISTRA EN EL DIRECTORIO FACILITADOR COMO TIPO "busqueda-peliculas"*/ 	
        DFAgentDescription descAgente = new DFAgentDescription();
        descAgente.setName(getAID());
        ServiceDescription descServ = new ServiceDescription();
        descServ.setType("busqueda-peliculas"); //setea el tipo de servicio
        descServ.setName("servicio-trakt");
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

                        // Generar la lista de películas de prueba simulando la consulta a la API de Trakt
                        ArrayList<Pelicula> encontradas = consumirTrakt(filtros);

                        // Preparar la respuesta estructurada
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        
                        // CRUCIAL: El ConversationId debe ser el mismo para mantener la trazabilidad asíncrona
                        reply.setConversationId(msg.getConversationId()); 
                        
                        // Serializar y empaquetar el objeto binario de respuesta
                        reply.setContentObject(encontradas);
                        send(reply);
                        
                        System.out.println(getLocalName() + ": Enviadas " + encontradas.size() + " películas de prueba al Planificador.");

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

    // Método principal que decide cómo buscar en Trakt según lo que pida el usuario.
    // Ahora recorre TODOS los géneros de FiltrosUsuario en vez de solo el primero.
    // Los géneros reconocidos (están en GENEROS_EN) van al endpoint de populares de Trakt.
    // Las keywords libres (ej: "star wars", "marvel") van al endpoint de búsqueda por título.
    private ArrayList<Pelicula> consumirTrakt(FiltrosUsuario filtros) {
        ArrayList<Pelicula> peliculas = new ArrayList<>();
        try {
            int anioMinimo = filtros.getAnio();

            // Clasificamos cada entrada del usuario: ¿es un género conocido o una keyword libre?
            ArrayList<String> generosConocidos = new ArrayList<>(); // se mandarán al endpoint popular
            ArrayList<String> keywords = new ArrayList<>();         // se buscarán por título

            if (filtros.getGeneros() != null) {
                for (String g : filtros.getGeneros()) {
                    String gl = g.trim().toLowerCase();
                    if (GENEROS_EN.containsKey(gl)) {
                        generosConocidos.add(GENEROS_EN.get(gl)); // traducimos al slug en inglés que acepta la API
                    } else if (!gl.isEmpty()) {
                        keywords.add(gl); // lo que no es género lo tratamos como búsqueda libre
                    }
                }
            }

            // Si el usuario no puso nada útil, buscamos películas populares en general
            if (generosConocidos.isEmpty() && keywords.isEmpty()) {
                keywords.add("popular");
            }

            Set<String> vistos = new HashSet<>(); // para no meter duplicados al combinar resultados
            ArrayList<Pelicula> pelis_sin_orden = new ArrayList<>();

            // Una sola llamada con todos los géneros juntos separados por coma (la API lo soporta)
            if (!generosConocidos.isEmpty()) {
                String generosJoined = String.join(",", generosConocidos);
                for (Pelicula p : buscarPorGeneroPopular(generosJoined, anioMinimo))
                    if (vistos.add(p.getNombre().toLowerCase())){
                        pelis_sin_orden.add(p);
                    }
            }

            // Una búsqueda por título por cada keyword libre
            for (String kw : keywords) {
                String kwEnc = URLEncoder.encode(kw, "UTF-8");
                for (Pelicula p : buscarPorTitulo(kwEnc, anioMinimo, null))
                    if (vistos.add(p.getNombre().toLowerCase())){
                        pelis_sin_orden.add(p);
                    }
            }

            // Ordenamos por puntuación y devolvemos el top 5
            // pelis_sin_orden.sort((a, b) -> Integer.compare(b.getPuntuacion(), a.getPuntuacion()));
            // for (int i = 0; i < Math.min(5, pelis_sin_orden.size()); i++) {
            //     peliculas.add(pelis_sin_orden.get(i));
            // }
            peliculas = pelis_sin_orden; // por ahora devolvemos todo sin ordenar para testear mejor la variedad de resultados
        } catch (Exception e) {
            System.err.println("[AgenteTrakt] Error: " + e.getMessage());
        }
        System.out.println("[AgenteTrakt] Total películas encontradas: " + peliculas.toString());
        return peliculas;
    }

    private ArrayList<Pelicula> buscarPorTitulo(String queryEnc, int anioMinimo, String filtroGenero) {
        ArrayList<Pelicula> lista = new ArrayList<>();
        String json = hacerPeticion("https://api.trakt.tv/search/movie?query=" + queryEnc + "&limit=25&extended=full");
        
        if (json == null || json.isBlank()) return lista;

        Gson gson = new Gson();
        // El endpoint de búsqueda devuelve un array de objetos intermedios que contienen el "score" y la "movie"
        TraktSearchItem[] items = gson.fromJson(json, TraktSearchItem[].class);

        for (TraktSearchItem item : items) {
            if (item != null && item.movie != null) {
                Pelicula p = transformarAModeloPelicula(item.movie, anioMinimo, filtroGenero);
                if (p != null) {
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    private ArrayList<Pelicula> buscarPorGeneroPopular(String generosCombinados, int anioMinimo) {
        ArrayList<Pelicula> lista = new ArrayList<>();
        
        String[] partes = generosCombinados.split(",");
        StringBuilder generosEnc = new StringBuilder();
        for (String parte : partes) {
            if (generosEnc.length() > 0) generosEnc.append(",");
            generosEnc.append(URLEncoder.encode(parte.trim(), StandardCharsets.UTF_8));
        }

        String urlStr = "https://api.trakt.tv/movies/popular?genres=" + generosEnc + "&limit=25&extended=full";
        if (anioMinimo > 1900) {
            urlStr += "&years=" + anioMinimo + "-2026";
        }

        String json = hacerPeticion(urlStr);
        if (json == null || json.isBlank()) {
            return lista;
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        // El endpoint popular devuelve un array directo de películas
        TraktMovie[] movies = gson.fromJson(json, TraktMovie[].class);

        for (TraktMovie movie : movies) {
            if (movie != null) {
                Pelicula p = transformarAModeloPelicula(movie, anioMinimo, null);
                if (p != null) {
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    // Método auxiliar para unificar la conversión desde el objeto de Trakt al objeto "Pelicula" común
    private Pelicula transformarAModeloPelicula(TraktMovie movie, int anioMinimo, String filtroGenero) {
        if (movie.title == null || movie.year < anioMinimo) {
            return null;
        }

        // Validar filtro de género específico si viene configurado
        if (filtroGenero != null && (movie.genres == null || !movie.genres.contains(filtroGenero))) {
            return null;
        }

        // Preparar lista de géneros
        List<String> listaGeneros = new ArrayList<>();
        if (movie.genres != null && !movie.genres.isEmpty()) {
            listaGeneros.addAll(movie.genres);
        } else {
            listaGeneros.add("Desconocido");
        }

        // Convertir la puntuación de escala 0-10 a escala 0-100
        int puntuacion = 60; // Valor neutro por defecto si no hay rating
        if (movie.rating != null && movie.rating > 0) {
            puntuacion = (int) Math.min(100, Math.max(10, movie.rating * 10));
        }

        String tituloCompleto = movie.title + " (" + movie.year + ")";
        String sinopsis = movie.overview != null ? movie.overview : "";

        return new Pelicula(tituloCompleto, listaGeneros, puntuacion, sinopsis);
    }

    private String hacerPeticion(String urlStr) {
        StringBuilder respuesta = new StringBuilder();
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("trakt-api-version", "2");
            conn.setRequestProperty("trakt-api-key", CLIENT_ID);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != 200) {
                System.err.println("[" + getLocalName() + "] HTTP Error " + conn.getResponseCode() + " en: " + urlStr);
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String linea;
            while ((linea = in.readLine()) != null) {
                respuesta.append(linea);
            }
            in.close();
            
        } catch (IOException e) {
            System.err.println("[" + getLocalName() + "] Error en la conexión I/O: " + e.getMessage());
            return null;
        }
        return respuesta.toString();
    }

    @Override
    protected void takeDown() {
        try { 
            DFService.deregister(this); 
            System.out.println("AgenteTrakt " + getLocalName() + " desregistrado del DF.");
        } catch (FIPAException fe) { 
            fe.printStackTrace(); 
        }
    }

    // CLASES DE MAPEADO PARA GSON (Modelos de la estructura JSON de Trakt)
    
    private static class TraktMovie {
        String title;
        int year;
        Double rating;
        List<String> genres;
        String overview;
    }

    private static class TraktSearchItem {
        Double score;
        TraktMovie movie;
    }
}