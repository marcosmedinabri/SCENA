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

                    String respuesta = "Si";
                    List<Tarea> listatareas = new ArrayList<>();

                    while(respuesta.equalsIgnoreCase("Si")) {
                        // Aqui va a ir la logica de preguntar al usuario por los datos, de momento terminal, luego pasar a swing ej clase
                        Scanner escaner = new Scanner(System.in);

                        System.out.println("Bienvenido al sistema inteligente para planificar tareas.");

                        System.out.println("Porfavor, introduzca el nombre de la tarea.");
                        String nombretarea = escaner.nextLine();

                        System.out.println("Introduzca del 1 al 5 como de importante es esa tarea.");
                        int importante = Integer.parseInt(escaner.nextLine());

                        System.out.println("Introduzca la fecha final para la tarea con el formato (DD-MM-YYYY).");
                        String fechalimite = escaner.nextLine();

                        System.out.println("Introduzca el numero de horas que se le quiere dedicar.");
                        int horastotales = escaner.nextInt();
                        escaner.nextLine();

                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                        sdf.setLenient(false); // Esto es para evitar fechas erroneas por si acaso segun documentacion de java

                        Date fechaformateada;
                        try {
                            fechaformateada = sdf.parse(fechalimite);
                        } catch (ParseException e) {
                            System.out.println("Error con el formato de la fecha, revisar.");
                            throw new RuntimeException(e);
                        }

                        Tarea tarea = new Tarea(nombretarea, importante, fechaformateada, horastotales);
                        System.out.println("Tarea registrada: " + tarea);
                        listatareas.add(tarea);

                        System.out.println("¿Tienes mas tareas pendientes por añadir? Si/No");
                        respuesta = escaner.nextLine().trim();

                    }


                    // Preparar el mensaje
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(planificadorAID);
                    
                    /*2. AQUI RECOGER LOS DATOS DEL USUAIO"*/
                    request.setContent("Comedia, Aventuras, Acción, 2025");
                    request.setConversationId("planificacion-semana-1"); //para identificar las conversaciones despues (esto hay q automatizarlo despues)

                    send(request);
                    System.out.println(getLocalName() + " ha enviado los datos");

                    // Se queda bloaqueado esperando la respuesta
                    // Queremos una respuesta a esta conversación y se espera el id "planificacion-semanal-1"
                    MessageTemplate mt = MessageTemplate.MatchConversationId("planificacion-semanal-1");
                    
                    System.out.println(getLocalName() + " esperando resultados de la planificación (bloqueado)...");
                    // El agente se detiene aquí hasta que llega un mensaje que cumpla el template
                    ACLMessage reply = blockingReceive(mt); 
                    
                    /*IMPRIME EL RESULTADO*/
                    //CAMBIAR MAS ADELANTE: Esto hay que hacerlo en un agente de output
                    if (reply != null) {  
                        System.out.println(getLocalName() + " RESULTADO FINAL MOSTRADO EN GUI: " + reply.getContent());
                    }
                } else {
                    System.out.println("No se encontró ningún agente planificador disponible.");
                }
            }
        });
    }
}