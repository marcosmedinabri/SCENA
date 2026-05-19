package es.upm.si.practica;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
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

public class AgenteTMDB extends Agent {

    @Override
    protected void setup() {
        System.out.println("AgenteTMDB " + getLocalName() + " iniciado.");

        DFAgentDescription descAgente = new DFAgentDescription();
        descAgente.setName(getAID());
        ServiceDescription descServ = new ServiceDescription();
        descServ.setType("busqueda-peliculas"); 
        descServ.setName("servicio-tmdb");
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
                        ArrayList<Pelicula> encontradas = raspearTMDB(filtros);
                        
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContentObject(encontradas);
                        send(reply);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }
        });
    }

    private ArrayList<Pelicula> raspearTMDB(FiltrosUsuario filtros) {
        ArrayList<Pelicula> peliculas = new ArrayList<>();
        try {
            String query = URLEncoder.encode(filtros.getGenero() + " site:themoviedb.org/movie", "UTF-8");
            URL url = new URL("https://news.google.com/rss/search?q=" + query + "&hl=es&gl=ES");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            if (conn.getResponseCode() == 200) {
                Scanner sc = new Scanner(conn.getInputStream(), "UTF-8");
                StringBuilder xml = new StringBuilder();
                while (sc.hasNextLine()) xml.append(sc.nextLine());
                sc.close();

                Matcher m = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL).matcher(xml.toString());
                int count = 0;

                while (m.find() && count < 3) {
                    String title = extraer(m.group(1), "title");
                    if (title != null) {
                        title = title.replace(" - The Movie Database (TMDB)", "").replace(" - Google Noticias", "");
                        // Usamos la clase Pelicula de tu grupo
                        Pelicula p = new Pelicula(title, Arrays.asList("Info provista por TMDB"), 85 - (count * 5));
                        peliculas.add(p);
                        count++;
                    }
                }
            }
        } catch (Exception e) {}
        return peliculas;
    }

    private String extraer(String xml, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL).matcher(xml);
        return m.find() ? m.group(1).replaceAll("<[^>]*>", "").trim() : null;
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException fe) { fe.printStackTrace(); }
    }
}