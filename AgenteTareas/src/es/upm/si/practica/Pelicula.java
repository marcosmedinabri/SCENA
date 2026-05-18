package es.upm.si.practica;

import java.io.Serializable;
import java.util.List;

// Serializable para que JADE pueda enviar este objeto entre agentes
public class Pelicula implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String nombre;
    private List<String> generos;
    private int puntuacion;

    // Constructor vacío (importante para serialización)
    public Pelicula() {
    }

    public Pelicula(String nombre, List<String> generos, int puntuacion) {
        this.nombre = nombre;
        this.generos = generos;
        this.puntuacion = puntuacion;
    }

    // GETTERS y SETTERS

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<String> getGeneros() {
        return generos;
    }

    public void setGeneros(List<String> generos) {
        this.generos = generos;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    // toString para debug y envío
    @Override
    public String toString() {
        return "Pelicula{" +
                "nombre='" + nombre + '\'' +
                ", generos=" + generos +
                ", puntuacion=" + puntuacion +
                '}';
    }
}