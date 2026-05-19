package es.upm.si.practica;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

//Así no hay que configurar todo el rato el run configuration :P
public class Main { 
    public static void main(String[] args) throws Exception {
        Runtime rt = Runtime.instance(); //instancia del runtime, creo que gestiona los contenedores y servicios y todo eso
        Profile p = new ProfileImpl(); //perfil de configuracion 
     
        p.setParameter(Profile.GUI, "true"); //para abrir la interfaz de JADE
        p.setParameter(Profile.LOCAL_PORT, "1202"); //puerto para JADE pero hay que ir cambiandolo porque a veces no funciona :/
        
        AgentContainer container = rt.createMainContainer(p); //crear el contenedor principal 

        // NUESTROS AGENTE!!!!
        AgentController planificador = container.createNewAgent("Planificador", AgentePlanificador.class.getName(), null);
        AgentController tmdb = container.createNewAgent("AgenteTMDB", AgenteTMDB.class.getName(), null);
        AgentController interfaz = container.createNewAgent("InterfazUsuario", AgenteInterfaz.class.getName(), null);

        // Arrancamos primero el planificador para que le dé tiempo a registrarse en el DF
        planificador.start();
        tmdb.start();
        // Pausa cortita para asegurar el registro antes de la búsqueda
        Thread.sleep(500); 
        
        interfaz.start(); //se lanza, a tope
    }
}