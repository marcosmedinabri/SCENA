package es.upm.si.practica;

import jade.core.Agent;


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