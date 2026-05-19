package es.upm.si.practica;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FiltrosUsuario implements Serializable {
    
    // Identificador único para la consistencia en la serialización binaria de JADE
    private static final long serialVersionUID = 1L;
    
    private List<String> generos;
    private int anio;

    public FiltrosUsuario() {
        this.generos = new ArrayList<>();
    }

    public FiltrosUsuario(List<String> generos, int anio) {
        this.generos = generos;
        this.anio = anio;
    }


    // GETTERS Y SETTERS

    public List<String> getGeneros() {
        return generos;
    }

    public void setGeneros(List<String> generos) {
        this.generos = generos;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public void addGenero(String genero) {
        if (this.generos == null) {
            this.generos = new ArrayList<>();
        }
        this.generos.add(genero);
    }

    @Override
    public String toString() {
        return "FiltrosUsuario{" +
                "generos=" + generos +
                ", anio=" + anio +
                '}';
    }
}