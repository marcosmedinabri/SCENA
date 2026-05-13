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
                
                /*Envia al agentePlanificador un mensaje REQUUEST con tareas*/
                
                if (planificadorAID != null) {
                    // Preparar el mensaje
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(planificadorAID);
                    //Ejemplo porq no tenemos interfaz para recoger datos todavía
                    request.setContent("Tarea1: Examen, Prioridad: Alta; Tarea2: Práctica, Prioridad: Media"); 
                    request.setConversationId("planificacion-semana-1"); //para identificar las conversaciones despues (esto hay q automatizarlo despues)
                    send(request);
                    System.out.println(getLocalName() + " ha enviado los datos de las tareas.");

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