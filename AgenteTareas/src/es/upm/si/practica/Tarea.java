package es.upm.si.practica;

import java.io.Serializable;
import java.util.Date;

//Serializable para que JADE pueda enviar este objeto entre agentes
public class Tarea implements Serializable {
    
    // Identificador único para la serialización
    private static final long serialVersionUID = 1L;

    private String nombre;
    private int criticidad; // 1 (muy critico) a 5 (poco critico)
    private Date deadline;  // Fecha límite
    private int horasDedicadas;


    //Constructor vacío requerido por muchas librerías de serialización (como Gson o Jackson)
    public Tarea() {
    }

    public Tarea(String nombre, int criticidad, Date deadline, int horasDedicadas) {
        this.nombre = nombre;
        this.criticidad = criticidad;
        this.deadline = deadline;
        this.horasDedicadas = horasDedicadas;
    }

 
    //GETERS y SETERS
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCriticidad() {
        return criticidad;
    }

    public void setCriticidad(int criticidad) {
        this.criticidad = criticidad;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public int getHorasDedicadas() {
        return horasDedicadas;
    }

    public void setHorasDedicadas(int horasDedicadas) {
        this.horasDedicadas = horasDedicadas;
    }

    //Metodo toString para facilitar el envio y el output
    public String toString() {
        return "Tarea{" +
                "nombre='" + nombre + '\'' +
                ", criticidad=" + criticidad +
                ", deadline=" + deadline +
                ", horasDedicadas=" + horasDedicadas +
                '}';
    }
}
