package es.upm.si.practica;


import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class AlgoritmoPlanificador {

    private static final double PESO_GENEROS = 5.0;
    private static final double PESO_VALORACION = 3.0;
    private static final double PESO_KEYWORDS = 1.5;

    public static void generarListaPelis(List<Pelicula> listaPelis, FiltrosUsuario filtros) {

        final List<String> palabrasClaveSipnosis = PalabrasClavePlanificador.procesado(filtros.getPreferencia()); //palabrasClaveSipnosis tiene las palabras clave de la preferencia del usuario
        //System.out.println(palabrasClaveSipnosis);

        if (listaPelis == null || listaPelis.isEmpty()) { //caso base
            return;
        }

        Collections.sort(listaPelis, new Comparator<Pelicula>() {
            @Override
            public int compare(Pelicula p2, Pelicula p1) {
                //se compara p2 con p1 (en lugar de p1 con p2) para ordenar de MAYOR a MENOR
                double score1 = calcularScore(p1, filtros, palabrasClaveSipnosis);
                double score2 = calcularScore(p2, filtros, palabrasClaveSipnosis);
                return Double.compare(score1, score2);
            }
        });
    }


    private static double calcularScore(Pelicula p, FiltrosUsuario filtros, List<String> keywords) {
        //Aqui se normalizan los generos del 0.0 al 1.1
        double scoreGeneros = 0.0;
        int generosCoincidentes = 0;
        List<String> generosUsuario = filtros.getGeneros(); // Se asume que devuelve List<String>

        if (generosUsuario != null && !generosUsuario.isEmpty() && p.getGeneros() != null) {
            for (String gUser : generosUsuario) {
                // Comparamos ignorando mayúsculas/minúsculas
                for (String gPeli : p.getGeneros()) {
                    if (gUser.equalsIgnoreCase(gPeli)) {
                        generosCoincidentes++;
                        break;
                    }
                }
            }
            scoreGeneros = (double) generosCoincidentes / generosUsuario.size();
        }

        // Aqui se normaliza la valoracion
        double scoreValoracion = (double) p.getPuntuacion() / 100.0;

        // Aqui se normalizan las keywords
        double scoreKeywords = 0.0;
        int keywordsEncontradas = 0;

        String sinopsisLimpia = p.getSinopsis() != null ? p.getSinopsis().toLowerCase() : "";

        if (keywords != null && !keywords.isEmpty() && !sinopsisLimpia.isEmpty()) {
            for (String kw : keywords) {
                if (sinopsisLimpia.contains(kw.toLowerCase())) {
                    keywordsEncontradas++;
                }
            }
            scoreKeywords = (double) keywordsEncontradas / keywords.size();
        }

        //se calcula la puntuacion final de la peli
        double puntuacionFinal = (scoreGeneros * PESO_GENEROS) + (scoreValoracion * PESO_VALORACION) + (scoreKeywords * PESO_KEYWORDS);

        return puntuacionFinal;
    }

}
