package es.upm.si.practica;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgenteTasteDive extends Agent {

    private static final String API_KEY  = "1072959-Movierec-F37CAA63";
    private static final String BASE_URL = "https://tastedive.com/api/similar";

    private static final Map<String, String> SEMILLA_GENERO = new HashMap<>();
    static {
        SEMILLA_GENERO.put("fantasia",        "The Lord of the Rings");
        SEMILLA_GENERO.put("fantasy",         "The Lord of the Rings");
        SEMILLA_GENERO.put("accion",          "Die Hard");
        SEMILLA_GENERO.put("action",          "Die Hard");
        SEMILLA_GENERO.put("terror",          "The Conjuring");
        SEMILLA_GENERO.put("horror",          "The Conjuring");
        SEMILLA_GENERO.put("comedia",         "The Hangover");
        SEMILLA_GENERO.put("comedy",          "The Hangover");
        SEMILLA_GENERO.put("drama",           "The Shawshank Redemption");
        SEMILLA_GENERO.put("thriller",        "The Silence of the Lambs");
        SEMILLA_GENERO.put("suspenso",        "The Silence of the Lambs");
        SEMILLA_GENERO.put("aventura",        "Indiana Jones");
        SEMILLA_GENERO.put("adventure",       "Indiana Jones");
        SEMILLA_GENERO.put("animacion",       "Toy Story");
        SEMILLA_GENERO.put("animation",       "Toy Story");
        SEMILLA_GENERO.put("romance",         "The Notebook");
        SEMILLA_GENERO.put("crimen",          "The Godfather");
        SEMILLA_GENERO.put("crime",           "The Godfather");
        SEMILLA_GENERO.put("misterio",        "Knives Out");
        SEMILLA_GENERO.put("mystery",         "Knives Out");
        SEMILLA_GENERO.put("documental",      "Free Solo");
        SEMILLA_GENERO.put("documentary",     "Free Solo");
        SEMILLA_GENERO.put("guerra",          "Saving Private Ryan");
        SEMILLA_GENERO.put("war",             "Saving Private Ryan");
        SEMILLA_GENERO.put("ciencia ficcion", "Interstellar");
        SEMILLA_GENERO.put("sci-fi",          "Interstellar");
        SEMILLA_GENERO.put("sci fi",          "Interstellar");
        SEMILLA_GENERO.put("oeste",           "The Good the Bad and the Ugly");
        SEMILLA_GENERO.put("western",         "The Good the Bad and the Ugly");
        SEMILLA_GENERO.put("biografia",       "The Social Network");
        SEMILLA_GENERO.put("historia",        "Gladiator");
        SEMILLA_GENERO.put("history",         "Gladiator");
    }

    @Override
    protected void setup() {
        System.out.println("[AgenteTasteDive] " + getLocalName() + " iniciado.");

        DFAgentDescription descAgente = new DFAgentDescription();
        descAgente.setName(getAID());
        ServiceDescription descServ = new ServiceDescription();
        descServ.setType("busqueda-peliculas");
        descServ.setName("servicio-tastedive");
        descAgente.addServices(descServ);

        try { DFService.register(this, descAgente); } catch (FIPAException fe) { fe.printStackTrace(); }

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    try {
                        FiltrosUsuario filtros = (FiltrosUsuario) msg.getContentObject();
                        ArrayList<Pelicula> encontradas = consumirTasteDive(filtros);

                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContentObject(encontradas);
                        send(reply);
                    } catch (Exception e) { e.printStackTrace(); }
                } else { block(); }
            }
        });
    }

    private ArrayList<Pelicula> consumirTasteDive(FiltrosUsuario filtros) {
        ArrayList<Pelicula> peliculas = new ArrayList<>();
        try {
            String entrada      = filtros.getGenero().trim();
            String entradaLower = entrada.toLowerCase();

            String query = SEMILLA_GENERO.getOrDefault(entradaLower, entrada);

            ArrayList<Pelicula> similares = buscarSimilares(query);

            Set<String> vistos = new HashSet<>();
            for (Pelicula p : similares) {
                if (vistos.add(p.getNombre().toLowerCase()) && peliculas.size() < 5) {
                    peliculas.add(p);
                }
            }
        } catch (Exception e) {
            System.err.println("[AgenteTasteDive] Error: " + e.getMessage());
        }
        return peliculas;
    }

    private String hacerPeticion(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) {
                System.err.println("[AgenteTasteDive] HTTP " + conn.getResponseCode() + " -- " + urlStr);
                return null;
            }
            Scanner sc = new Scanner(conn.getInputStream(), "UTF-8");
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) sb.append(sc.nextLine());
            sc.close();
            return sb.toString();
        } catch (Exception e) {
            System.err.println("[AgenteTasteDive] Error peticion: " + e.getMessage());
            return null;
        }
    }

    private ArrayList<Pelicula> buscarSimilares(String query) {
        ArrayList<Pelicula> lista = new ArrayList<>();
        try {
            String queryEnc = URLEncoder.encode(query, "UTF-8");
            String url = BASE_URL + "?q=" + queryEnc + "&type=movie&limit=20&info=0&k=" + API_KEY;
            String json = hacerPeticion(url);
            if (json == null) return lista;

            int resultsIdx = json.indexOf("\"results\":");
            if (resultsIdx == -1) return lista;
            String resultadosJson = json.substring(resultsIdx);

            Matcher m = Pattern.compile("\"name\":\"([^\"]+)\"").matcher(resultadosJson);

            int puntuacion = 90;
            while (m.find() && lista.size() < 10) {
                String nombre = m.group(1);
                System.out.println("[TasteDive] " + nombre + " score=" + puntuacion);
                lista.add(new Pelicula(nombre, Arrays.asList("TasteDive"), puntuacion));
                puntuacion = Math.max(50, puntuacion - 5);
            }
        } catch (Exception e) {
            System.err.println("[AgenteTasteDive] Error busqueda: " + e.getMessage());
        }
        return lista;
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException fe) { fe.printStackTrace(); }
    }
}
