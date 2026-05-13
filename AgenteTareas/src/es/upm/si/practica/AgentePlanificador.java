package es.upm.si.practica;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgentePlanificador extends Agent {

    @Override
    protected void setup() {
        System.out.println("Planificador " + getLocalName() + " iniciado.");

        /*AQUI EL AGENTE SE REHISTRA EN EL DIRECTORIO FACILITADOR COMO TIPO "plaificacion"*/
        DFAgentDescription descAgente = new DFAgentDescription();
        descAgente.setName(getAID());
        ServiceDescription descServ = new ServiceDescription();
        descServ.setType("planificacion"); //setea el tipo de servicio
        descServ.setName("Servicio-Planificacion-Semanal");
        descAgente.addServices(descServ);

        try {
            DFService.register(this, descAgente);
            System.out.println(getLocalName() + " registrado en el DF con éxito.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Comportamiento cíclico
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                // Espera las peticiones (REQUEST)
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt); //mensaje del AgenteInterfaz

                if (msg != null) {
                    System.out.println(getLocalName() + " ha recibido tareas para procesar: " + msg.getContent());
                    
                    /*AQUI IRIA LA LOGICA DE LAS OPERACIONES Y ESO*/
                    String resultadoPlanificacion = "Lunes: Examen SI, Martes: Entrega JADE";
                    
                    /*RESPUESTA A QUIEN HA ENVIADO LA PETICION*/
                    ACLMessage reply = msg.createReply(); //incluye el identificador
                    reply.setPerformative(ACLMessage.INFORM); //devuelve el resultado
                    reply.setContent(resultadoPlanificacion);
                    send(reply);
                    System.out.println(getLocalName() + " ha enviado la planificación generada.");
                } else {
                    // Si no hay mensajes, bloqueamos el comportamiento hasta que llegue uno
                    block();
                }
            }
        });
    }

    // Es buena práctica desregistrar el agente cuando se destruye
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("Planificador " + getLocalName() + " desregistrado del DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}