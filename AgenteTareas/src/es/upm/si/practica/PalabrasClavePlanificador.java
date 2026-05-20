package es.upm.si.practica;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class PalabrasClavePlanificador {
	private static final Set<String> STOPWORDS_ES = new HashSet<>(Arrays.asList(
            "el", "la", "lo", "los", "las", "un", "una", "unos", "unas", "a", "ante", "bajo", 
            "con", "contra", "de", "desde", "en", "entre", "hacia", "hasta", "para", "por", 
            "según", "sin", "sobre", "tras", "y", "e", "ni", "que", "pero", "aunque", 
            "porque", "pues", "yo", "tú", "él", "ella", "nosotros", "vosotros", "ellos", "me", "te", 
            "se", "le", "nos", "os", "les", "no", "sí", "muy", "mucho", 
            "poco", "más", "menos", "bien", "mal", "acerca", "además", "al", "algo", "alguna", 
            "algunas", "alguno", "algunos", "antes", "aquel", "aquella", "aquellas", "aquello", 
            "aquellos", "aquí", "cada", "casi", "como", "cual", "cuales", "cualquier", "cuando", 
            "donde", "era", "eran", "eres", "es", "esa", "esas", "ese", "eso", "esos", "está", 
            "estamos", "están", "estás", "este", "esto", "estos", "mi", "mis", "muchos", "nosotras", 
            "nuestra", "nuestras", "nuestro", "nuestros", "o", "quien", "quienes", "qué", "sea", 
            "sean", "seas", "ser", "también", "tanto", "tiene", "tienen", "tienes", "usted", "ustedes", "ya",
            
            // STOPWORDS DE DOMINIO CINEMATOGRÁFICO Y PETICIONES (Palabras simples)
            "quiero", "ver", "busca", "busco", "buscame", "quisiera", "gustaría", "gustaria", 
            "apetece", "apetecería", "verme", "vería", "veria", "ponme", "pon", "poner",
            "muéstrame", "muestrame", "mostrar", "enséñame", "enseñame", "enseñar", "recomiéndame", 
            "recomiendame", "recomendar", "buscar", "necesito", "necesitaría", "necesitaria", "estoy", 
            "estaba", "ganas", "pelicula", "peliculas", "peli", "peliculon", "film", "filme", "historia", "trama", "cuenta", 
            "relata"
    ));
	
	/*ESTE METODO DEVUELVE TOKENS DEL TEXTO DE ENTRADA*/
	public static List<String> procesado (String entrada){
		List<String>resultado = new ArrayList<>();
		
		if (entrada == null || entrada.trim().isEmpty()) {
            return resultado;
        }
		
		//Normalizar 
		String textoLimpio = Normalizer.normalize(entrada, Normalizer.Form.NFD);
		Pattern patronAcentos = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        textoLimpio = patronAcentos.matcher(textoLimpio).replaceAll(""); // Quita acentos
        
        // Quitar signos de puntuacion y acentospasar a minusculas
        textoLimpio = textoLimpio.replaceAll("[\\p{Punct}&&[^,]]", "").toLowerCase();
        
        //Hacer tokens por espacios
        String[] tokens = textoLimpio.split("\\s+");
        
        for (String token : tokens) {
            if (token.length() <= 3) { //si la palabra es de menos de tres caracteres se quita
                continue;
            }

            if (STOPWORDS_ES.contains(token)) { //filtrar por la lista de stopwords español
                continue;
            }
            
            resultado.add(token);
        }
        return resultado;
    }
}
