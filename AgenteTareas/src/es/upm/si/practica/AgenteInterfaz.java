package es.upm.si.practica;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class AgenteInterfaz extends Agent {

    @Override
    protected void setup() {
        System.out.println("agente de Interfaz " + getLocalName() + " iniciada.");

        addBehaviour(new OneShotBehaviour(this) { //esto es para ejecutarse solo una vez
            @Override
            public void action() {
                
            	/*Busca en el Directorio Facilitador agente que de servicio "planificacion"*/
            	
                DFAgentDescription desc = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("planificacion");
                desc.addServices(sd);
                
                AID planificadorAID = null;
                
                try {
                    System.out.println(getLocalName() + ": se está buscando el agente...");
                    DFAgentDescription[] result = DFService.search(myAgent, desc);
                    if (result.length > 0) {
                        planificadorAID = result[0].getName(); // Se coge el primer agente encontrado (por si acaso queremos añadir más, no se xd)
                        System.out.println("Agente planificador de tareas encontrado!: " + planificadorAID.getLocalName());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                
                /*Envia al agentePlanificador un mensaje REQUEST con tareas*/
                
                if (planificadorAID != null) {
                	//RECOGER LOS DATOS POR CONSOLA (Por ahora, luego ya java swing)
                	Scanner escaner = new Scanner(System.in);
                    System.out.println("Bienvenido a tu recomendador inteligente de peliculas.");

                    System.out.println("Porfavor, introduzca la lista de generos separados por ','");
                    String entradaGeneros = escaner.nextLine();
                    String[] generosArray = entradaGeneros.split("\\s*,\\s*"); //separar los generos
                    List<String> listaGeneros = new ArrayList<>();
                    for (String g : generosArray) {
                        if (!g.isEmpty()) {
                            listaGeneros.add(g);
                        }
                    }
                    
                    System.out.println("Introduzca el año a partrir del cual quiere obtener la lista.");
                    int anio = Integer.parseInt(escaner.nextLine());
                    
                    FiltrosUsuario filtros = new FiltrosUsuario(listaGeneros, anio);
                    
                    // Preparar el mensaje
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(planificadorAID);
                    
                    /*2. AQUI RECOGER LOS DATOS DEL USUAIO"*/
                    try {
						request.setContentObject(filtros);
					} catch (IOException e) {
						e.printStackTrace();
					}
                    request.setConversationId("planificacion-1"); //para identificar las conversaciones despues (esto hay q automatizarlo despues)

                    send(request);
                    System.out.println(getLocalName() + " ha enviado los datos");

                    // Se queda bloaqueado esperando la respuesta
                    // Queremos una respuesta a esta conversación y se espera el id "planificacion-semanal-1"
                    MessageTemplate mt = MessageTemplate.MatchConversationId("planificacion-1");
                    
                    System.out.println(getLocalName() + " esperando resultados de la búsqueda (bloqueado)...");
                    // El agente se detiene aquí hasta que llega un mensaje que cumpla el template
                    ACLMessage reply = blockingReceive(mt); 
                    
                    /*IMPRIME EL RESULTADO*/
                    //CAMBIAR MAS ADELANTE: Esto hay que hacerlo en un agente de output
                    if (reply != null) {  
                        try {
							System.out.println(getLocalName() + " RESULTADO FINAL MOSTRADO EN GUI: " + reply.getContentObject());
						} catch (UnreadableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                } else {
                    System.out.println("No se encontró ningún agente planificador disponible.");
                }
            }
        });
    }
}