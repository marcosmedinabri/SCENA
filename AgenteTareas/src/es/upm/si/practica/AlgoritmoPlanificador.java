package es.upm.si.practica;

import java.util.ArrayList;
import java.util.List;

public class AlgoritmoPlanificador {
    
    public static ArrayList<Pelicula> generarListaPelis(List<Pelicula> listaPelis, FiltrosUsuario filtros) {
        // Creamos la lista resultado basada en lo que encontraron los sensores
        ArrayList<Pelicula> resultado = new ArrayList<>(listaPelis);
        
        // ORDENACIÓN BÁSICA: De mayor a menor puntuación
        resultado.sort((p1, p2) -> Integer.compare(p2.getPuntuacion(), p1.getPuntuacion()));
        
        return resultado;
    }
}