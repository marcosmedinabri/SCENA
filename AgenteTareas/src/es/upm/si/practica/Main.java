package es.upm.si.practica;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main { 
    public static void main(String[] args) throws Exception {
        Runtime rt = Runtime.instance(); 
        Profile p = new ProfileImpl(); 
     
        p.setParameter(Profile.GUI, "true"); 
        p.setParameter(Profile.LOCAL_PORT, "1201"); 
        AgentContainer container = rt.createMainContainer(p); 

        // NUESTROS AGENTES
        AgentController planificador = container.createNewAgent("Planificador", AgentePlanificador.class.getName(), null);
        AgentController sensorTMDB   = container.createNewAgent("AgenteTMDB", AgenteTMDB.class.getName(), null);
        AgentController interfaz     = container.createNewAgent("InterfazUsuario", AgenteInterfaz.class.getName(), null);

        // Arrancamos primero el cerebro y los sensores para que se registren en el DF
        planificador.start();
        sensorTMDB.start();
        
        Thread.sleep(600); 
        
        // Arrancamos la interfaz gráfica la última
        interfaz.start(); 
    }
}