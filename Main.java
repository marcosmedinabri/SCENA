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
        p.setParameter(Profile.LOCAL_PORT, "1202");
        AgentContainer container = rt.createMainContainer(p);

        AgentController planificador    = container.createNewAgent("Planificador", AgentePlanificador.class.getName(), null);
        AgentController sensorTasteDive = container.createNewAgent("AgenteTasteDive", AgenteTasteDive.class.getName(), null);
        AgentController interfaz        = container.createNewAgent("InterfazUsuario", AgenteInterfaz.class.getName(), null);

        planificador.start();
        sensorTasteDive.start();

        Thread.sleep(800);

        interfaz.start();
    }
}
