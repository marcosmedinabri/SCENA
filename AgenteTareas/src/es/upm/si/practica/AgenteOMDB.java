package es.upm.si.practica;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

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
     * Consulta la API de OMDB con el título y extrae la URL del poster del JSON.
     * @return URL del poster, o null si no se encuentra o hay error.
     */
    private String obtenerUrlPoster(String titulo) {
        try {
            String urlStr = "https://www.omdbapi.com/?t="
                    + URLEncoder.encode(titulo, "UTF-8")
                    + "&apikey=" + OMDB_KEY;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea);
            }
            br.close();

            String json = sb.toString();
            int idx = json.indexOf("\"Poster\":\"");
            if (idx == -1) return null;

            String url = json.substring(idx + 10, json.indexOf("\"", idx + 10));
            if (url.equals("N/A") || url.trim().isEmpty()) return null;

            return url;

        } catch (Exception e) {
            System.err.println("[AgenteOMDB] Error al consultar OMDB para '" + titulo + "': " + e.getMessage());
            return null;
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
