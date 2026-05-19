package es.upm.si.practica;
import java.io.Serializable;

public class FiltrosUsuario implements Serializable {
    private static final long serialVersionUID = 1L;
    private String genero;
    private int anio;

    public FiltrosUsuario(String genero, int anio) {
        this.genero = genero;
        this.anio = anio;
    }
    
    // Getters necesarios para que los agentes puedan leer los datos
    public String getGenero() {
        return genero;
    }

    public int getAnio() {
        return anio;
    }
}