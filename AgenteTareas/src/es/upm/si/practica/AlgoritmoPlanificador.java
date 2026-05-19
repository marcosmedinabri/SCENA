package es.upm.si.practica;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class AlgoritmoPlanificador {
	 public static List<Pelicula> generarListaPelis(List<Pelicula> listaPelis, FiltrosUsuario filtros) {
		 List<Pelicula> resultado = new ArrayList<>();
		 
		 resultado.add(new Pelicula("Kung Fu Panda", Arrays.asList("acción", "infantil"), 4 ) );
		 return listaPelis;
	 }
}
