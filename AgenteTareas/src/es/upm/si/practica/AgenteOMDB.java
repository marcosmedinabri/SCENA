package es.upm.si.practica;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

/**
 * AgenteOMDB — consulta la API de OMDB para obtener la URL del poster de una película.
 *
 * Protocolo:
 *   - Recibe: REQUEST con el título de la película como contenido de texto.
 *   - Responde: INFORM con la URL del poster, o FAILURE si no se encuentra.
 */
public class AgenteOMDB extends Agent {

    private static final String OMDB_KEY = "b938e3e8";
    // Patrón para detectar y extraer el año del formato "Título (YYYY)" que manda AgenteTrakt
    private static final Pattern PATRON_ANIO = Pattern.compile("^(.+?)\\s*\\((\\d{4})\\)\\s*$");

    @Override
    protected void setup() {
        System.out.println("[AgenteOMDB] " + getLocalName() + " iniciado.");

        // Registrarse en el Directory Facilitator como servicio de búsqueda de posters
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("busqueda-posters");
        sd.setName("servicio-omdb");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println("[AgenteOMDB] Registrado en el DF como 'busqueda-posters'.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Comportamiento cíclico: atender peticiones de poster
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String titulo = msg.getContent();
                    System.out.println("[AgenteOMDB] Buscando poster para: " + titulo);

                    ACLMessage reply = msg.createReply();
                    reply.setConversationId(msg.getConversationId());

                    String posterUrl = obtenerUrlPoster(titulo);

                    if (posterUrl != null) {
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent(posterUrl);
                        System.out.println("[AgenteOMDB] Poster encontrado para '" + titulo + "'.");
                    } else {
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Poster no encontrado: " + titulo);
                        System.out.println("[AgenteOMDB] No se encontró poster para '" + titulo + "'.");
                    }

                    send(reply);
                } else {
                    block();
                }
            }
        });
    }

    /**
     * Busca el poster de una película con varios intentos en cascada:
     *  1. Título limpio + año exacto (?t= &y=)  → cubre títulos en inglés de Trakt con "(YYYY)"
     *  2. Título limpio sin año (?t=)            → cubre títulos sin año y variantes
     *  3. Búsqueda por query (?s=)               → más tolerante; ayuda con títulos en español
     *  4. Solo el título antes de ":" (?s=)      → fallback para subtítulos raros
     */
    private String obtenerUrlPoster(String titulo) {
        // --- Extraer año si el título viene en formato "Título (YYYY)" ---
        String tituloLimpio = titulo.trim();
        String anio = null;
        Matcher m = PATRON_ANIO.matcher(tituloLimpio);
        if (m.matches()) {
            tituloLimpio = m.group(1).trim();
            anio = m.group(2);
        }

        // Intento 1: coincidencia exacta con año (el más preciso)
        if (anio != null) {
            String json = llamarHTTP("https://www.omdbapi.com/?t="
                    + encode(tituloLimpio) + "&y=" + anio + "&apikey=" + OMDB_KEY);
            String poster = extraerPoster(json);
            if (poster != null) return poster;
        }

        // Intento 2: coincidencia exacta sin año
        String json2 = llamarHTTP("https://www.omdbapi.com/?t="
                + encode(tituloLimpio) + "&apikey=" + OMDB_KEY);
        String poster2 = extraerPoster(json2);
        if (poster2 != null) return poster2;

        // Intento 3: búsqueda por query — más tolerante con acentos y variantes en español
        String json3 = llamarHTTP("https://www.omdbapi.com/?s="
                + encode(tituloLimpio) + "&type=movie&apikey=" + OMDB_KEY);
        String poster3 = extraerPoster(json3);
        if (poster3 != null) return poster3;

        // Intento 4: solo el título antes de ":" por si el subtítulo rompe la búsqueda
        if (tituloLimpio.contains(":")) {
            String tituloCorto = tituloLimpio.substring(0, tituloLimpio.indexOf(":")).trim();
            String json4 = llamarHTTP("https://www.omdbapi.com/?s="
                    + encode(tituloCorto) + "&type=movie&apikey=" + OMDB_KEY);
            String poster4 = extraerPoster(json4);
            if (poster4 != null) return poster4;
        }

        return null;
    }

    /** Hace una petición HTTP GET y devuelve el cuerpo como String, o null si hay error. */
    private String llamarHTTP(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            if (conn.getResponseCode() != 200) return null;

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) sb.append(linea);
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Extrae el primer valor del campo "Poster" de un JSON de OMDB. Devuelve null si es "N/A" o no existe. */
    private String extraerPoster(String json) {
        if (json == null) return null;
        int idx = json.indexOf("\"Poster\":\"");
        if (idx == -1) return null;
        String valor = json.substring(idx + 10, json.indexOf("\"", idx + 10));
        if (valor.equals("N/A") || valor.trim().isEmpty()) return null;
        return valor;
    }

    /** URLEncoder wrapper para no repetir el try/catch. */
    private String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("[AgenteOMDB] " + getLocalName() + " desregistrado del DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
