package es.upm.si.practica;

import java.util.List;


// Engloba a las respuesta total del json al llamar a TMDB, y luego a partir de results pues obtenemos las pelis :)
public class ConjuntoTMDB {

    // Sinceramente lo pongo en ingles para no poner lo del serializedName que me da pereza

    private List<PeliculaTMDB> results;

    public List<PeliculaTMDB> getResults() {
        return results;
    }

    public void setResults(List<PeliculaTMDB> results) {
        this.results = results;
    }
}
