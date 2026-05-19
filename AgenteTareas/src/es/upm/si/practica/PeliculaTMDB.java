package es.upm.si.practica;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

// Este model es para mapear la respuesta de TMDB, y luego ya lo convertimos a Pelicula normal para enviarlo al planificador
// pero bueno, asi es mas facil mapearlo con el gson (Si es casi igual a Pelicula pero asi no modifico la clase principial de Pelicula)
public class PeliculaTMDB implements Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName("title")
    private String nombre;
    @SerializedName("genre_ids")
    private List<String> generos;
    @SerializedName("vote_average")
    private float puntuacion;

    // Constructor vacío (importante para serialización)
    public PeliculaTMDB() {
    }

    public PeliculaTMDB(String nombre, List<String> generos, float puntuacion) {
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

    public float getPuntuacion() {
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
